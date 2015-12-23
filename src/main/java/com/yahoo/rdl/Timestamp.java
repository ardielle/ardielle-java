/**
 * Copyright 2015 Yahoo Inc.
 * Licensed under the terms of the Apache version 2.0 license. See LICENSE file for terms.
 */

package com.yahoo.rdl;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Date;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * The RDL Timestamp primitive type
 */
@JsonSerialize(using = Timestamp.TimestampJsonSerializer.class)
@JsonDeserialize(using = Timestamp.TimestampJsonDeserializer.class)
public class Timestamp {
    String repr;
    Timestamp(String s) {
        repr = s;
    }
    public static class TimestampJsonSerializer extends JsonSerializer<Timestamp> {
        @Override
        public void serialize(Timestamp value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(value.toString());
        }
    }
    public static class TimestampJsonDeserializer extends JsonDeserializer<Timestamp> {
        @Override
        public Timestamp deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String s = jp.getText();
            return Timestamp.fromString(s);
        }
    }

    private static String num(int n, int width) {
        String s = String.valueOf(n);
        while (s.length() < width) {
            s = "0" + s;
        }
        return s;
    }
    static private Date rfc3339ToDate(String rfc3339) {
        String s = rfc3339;
        if (s == null || s.length() < 19) { 
            return null;
        }
        if (!s.endsWith("Z")) {
            int i = s.lastIndexOf('+');
            if (i < 0) {
                i = s.lastIndexOf('-');
            }
            s = s.substring(0, i + 3) + rfc3339.substring(i + 4);
        } else {
            //Special case: SimpleDateFormat with "Z" does not accept rfc3339 ending with "Z" (GMT timezone)
            //To handle this special case, there are two options:
            //Option 1: calendar is used to parse GMT timezone rfc3339 string
            //          final Calendar calendar = javax.xml.bind.DatatypeConverter.parseDateTime(rfc3339);
            //          return calendar.getTime();
            //Option 2: Another approach is to replace "Z" ending character in rfc3339 by "-0000".
            s = s.substring(0, s.length() - 1) + "-0000";
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ").parse(s);
        } catch (java.text.ParseException e) {
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
        } catch (java.text.ParseException e) {
        }
        return null;
    }
    public static Timestamp fromObject(Object o) {
        if (o instanceof Timestamp) {
            return (Timestamp) o;
        } else if (o instanceof String) {
            return fromString((String) o);
        } else if (o instanceof Long) {
            return fromMillis(((Long) o).longValue());
        } else if (o instanceof Date) {
            return fromDate((Date) o);
        } else {
            throw new ClassCastException("Not a Timestamp: " + o.getClass());
        }
    }
    public static Timestamp fromString(String s) {
        Date d = rfc3339ToDate(s);
        if (d == null) {
            return null;
        }
        return Timestamp.fromMillis(d.getTime());
    }
    public static Timestamp fromCurrentTime() {
        long millis = System.currentTimeMillis();
        return fromMillis(millis);
    }
    public static Timestamp fromMillis(long millis) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(millis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);
        int ms = cal.get(Calendar.MILLISECOND);
        String s = year + "-" + num(month, 2) + "-" + num(day, 2) + "T" + num(hour, 2) + ":" + num(min, 2) + ":" + num(sec, 2) + "." + num(ms, 3) + "Z";
        return new Timestamp(s);
    }
    /**
     * Create and return a new Timestamp from the java.util.Date instance.
     * @param d java.util.Date instance
     * @return Timestamp object representing the d
     */
    public static Timestamp fromDate(Date d) {
        return new Timestamp(rfc3339FromDate(d));
    }
    public Date toDate() {
        return rfc3339ToDate(repr);
    }
    static private String rfc3339FromDate(Date d) {
        String iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ").format(d);
        if (iso8601.endsWith("Z")) {
            return iso8601;
        }
        int len = iso8601.length();
        return iso8601.substring(0, len - 2) + ":" + iso8601.substring(len - 2);
    }

    static private Date rfc1123ToDate(String rfc1123) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.parse(rfc1123, new ParsePosition(0));
    }

    static private String rfc1123FromDate(Date d) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss zzz");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(d);
    }

    public String toString() {
        return repr;
    }
    public boolean equals(Object another) {
        if (another instanceof Timestamp) {
            return repr.equals(another.toString());
        }
        return false;
    }
    private static final long UNINITIALIZED = 0x7fffffffffffffffL;
    private long normalizedMillis = UNINITIALIZED;

    public long millis() {
        if (normalizedMillis == UNINITIALIZED) {
            synchronized (this) {
                if (normalizedMillis == UNINITIALIZED) {
                    Date d = rfc3339ToDate(repr);
                    if (d == null) {
                        return 0;
                    }
                    normalizedMillis = d.getTime();
                }
            }
        }
        return normalizedMillis;
    }
}
