package GlobalClasses;

import java.util.Calendar;

public class DateHelper {

    public int getNextWeekDate()
    {

        Calendar cal=Calendar.getInstance();
        if(cal.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY){
            cal.add(Calendar.DATE, 7);
        }else{
            while(cal.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY){
                cal.add(Calendar.DATE,1);
            }
        }

        return formatDate(cal);

    }





    private int formatDate(Calendar cal){
        int year=cal.get(Calendar.YEAR);
        int month=cal.get(Calendar.MONTH)+1;
        int day=cal.get(Calendar.DAY_OF_MONTH);
        return year*10000+month*100+day;
    }


    /**
     *
     * @param date - date to be checked.
     * @throws Exception when date is in wrong format (correct format = YYYYMMDD) or date is not sunday of the given week
     */

    public void checkDateValidity(int date) throws Exception {
        String dateString = String.valueOf(date);
        if (dateString.length() != 8) {
            throw new Exception("Invalid date format, should be: YYYYMMDD");
        }
        int year = Integer.parseInt(dateString.substring(0, 4));
        int month = Integer.parseInt(dateString.substring(4, 6));
        int day = Integer.parseInt(dateString.substring(6, 8));
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);
        cal.set(year, month - 1, day);
        try {
            cal.getTime();
        } catch (Exception e) {
            throw new Exception("Invalid date");
        }
        if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            throw new Exception("The date provided is not a Sunday");
        }
    }




    /**
     *
     * @param currWeekDate - assumed to be sunday of (any) week.
     * @return sunday of next week from currWeekDate.
     */
    public int getNextWeekDate(int currWeekDate) {
        Calendar cal=Calendar.getInstance();
        cal.set(Integer.parseInt(String.valueOf(currWeekDate).substring(0,4)),
                Integer.parseInt(String.valueOf(currWeekDate).substring(4,6))-1,
                Integer.parseInt(String.valueOf(currWeekDate).substring(6,8)));

        cal.add(Calendar.DATE,7);
        return formatDate(cal);


    }

    public int getDayOfWeek()
    {
        //TODO: return current day. i'll assume 0=sunday,1=monday...6=saturday.
        // i.e. for june 6th 2024, should return 4 (=thursday).
        Calendar cal=Calendar.getInstance();
        return cal.get(Calendar.DAY_OF_WEEK)-1;

    }
}
