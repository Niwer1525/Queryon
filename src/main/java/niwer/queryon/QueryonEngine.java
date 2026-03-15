package niwer.queryon;

import java.text.SimpleDateFormat;
import java.util.Date;

import niwer.lumen.LumenEngine;
import niwer.lumen.container.Container;

public class QueryonEngine {

    public static final Container LOGGER = LumenEngine.registerContainer("QUERYON");

    public static String dateToSQL(Date date) {
        final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
        return FORMATTER.format(date);
    }

    public static String dateTimeToSQL(Date date) {
        final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return FORMATTER.format(date);
    }
}
