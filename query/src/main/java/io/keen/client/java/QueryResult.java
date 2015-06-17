package io.keen.client.java;


import java.lang.reflect.Array;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.Query;

/**
 * Created by claireyoung on 6/16/15.
 */
public class QueryResult {
    private Integer integer;
    private Double doubleValue;
    private String str;
    private GroupBy groupBy;
    private Interval interval;
    private ArrayList<QueryResult> list;    // TODO: Will this ever be a List but NOT an ArrayList?
    private Object object;  // to be used carefully!!!


    // Constructors - they are all Private!
    private QueryResult(Integer integer) { this.integer = integer; }

    private QueryResult(Double doubleVal) { this.doubleValue = doubleVal; }

    private QueryResult(GroupBy groupBy) { this.groupBy = groupBy; }

    private QueryResult(String str) { this.str = str; }

    private QueryResult(Interval interval) { this.interval = interval; }

    private QueryResult(ArrayList<QueryResult> list) {
        this.list = list;
    }

    private QueryResult(Object object) {
        this.object = object;
    }

    // validation methods

    public boolean isInteger() {
        return integer != null;
    }

    public boolean isDouble() {
        return doubleValue != null;
    }

    public boolean isGroupBy() { return groupBy != null; }

    public boolean isInterval() { return interval != null; }

    public boolean isList() { return list != null; }

    public boolean isString() { return str != null; }

    // getters
    public Integer getInteger() {
        return integer;
    }

    public Double getDouble() {
        return doubleValue;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public String getString() {return str;}

    public Interval getInterval() { return interval; }

    public Object getObject() { return object; }

    public ArrayList<QueryResult> getList() {
        return list;
    }

    // Construct Query Result
    public static QueryResult constructQueryResult(Object input, boolean isGroupBy, boolean isInterval) {
        QueryResult thisObject = null;
        if (input instanceof Integer) {
            thisObject = new QueryResult((Integer)input);
        } else if (input instanceof Double) {
            thisObject = new QueryResult((Double)input);
        } else if (input instanceof String) {
            thisObject = new QueryResult((String)input);
        } else if (input instanceof ArrayList) {

            // recursively construct the children of this...
            ArrayList<QueryResult> listOutput = new ArrayList<QueryResult>();
            ArrayList<Object> listInput = (ArrayList<Object>)input;
            for (Object child : listInput) {
                QueryResult resultItem = constructQueryResult(child, isGroupBy, isInterval);
                listOutput.add(resultItem);
            }
            thisObject = new QueryResult(listOutput);
        } else {
            if (input instanceof HashMap) {
                HashMap<String, Object> inputMap = (HashMap<String, Object>) input;

                // if there is an interval or groupBy, we expect to process them at
                // the top level. When we recurse, we want to just make sure that
                // we don't have any nested Intervals or GroupBy's by explicitly setting
                // them to false.
                if (isInterval) {

                    // If this is an interval, it should have keys "timeframe" and "value"
                    if (inputMap.containsKey(KeenQueryConstants.TIMEFRAME) && inputMap.containsKey(KeenQueryConstants.VALUE)) {
                        Timeframe timeframeOutput = null;
                        Object timeframe = inputMap.get(KeenQueryConstants.TIMEFRAME);
                        if (timeframe instanceof HashMap) {
                            HashMap<String, String> hashTimeframe = (HashMap<String, String>) timeframe;
                            String start = hashTimeframe.get(KeenQueryConstants.START);
                            String end = hashTimeframe.get(KeenQueryConstants.END);
                            timeframeOutput = new Timeframe(start, end);
                        } else if (timeframe instanceof String) {
                            timeframeOutput = new Timeframe((String) timeframe);
                        }

                        Object value = inputMap.get(KeenQueryConstants.VALUE);
                        QueryResult queryResultValue = constructQueryResult(value, isGroupBy, false);
                        thisObject = new QueryResult(new Interval(timeframeOutput, queryResultValue));
                    }
                } else if (isGroupBy) {

                    // If this is a GroupBy, it should have key "result", along with properties to group by.
                    if (inputMap.containsKey(KeenQueryConstants.RESULT)) {
                        QueryResult result = null;
                        HashMap<String, Object> properties = new HashMap<String, Object>();
                        for (String key : inputMap.keySet()) {
                            if (key.equals(KeenQueryConstants.RESULT)) {
                                // there should not be intervals nested inside GroupBy's; only
                                // the other way around.
                                result = constructQueryResult(inputMap.get(key), false, false);
                            } else {
                                properties.put(key, inputMap.get(key));
                            }
                        }
                        thisObject = new QueryResult(new GroupBy(properties, result));
                    }
                }
            }
        }

        // this is a catch-all for Select Unique queries, where the object can be of any type.
        if (thisObject == null) {
            thisObject = new QueryResult(input);
        }

        return thisObject;
    }

}
