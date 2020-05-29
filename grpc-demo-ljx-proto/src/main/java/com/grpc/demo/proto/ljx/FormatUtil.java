package com.grpc.demo.proto.ljx;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.MapDeserializer;
import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessageV3;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class FormatUtil {
    /**
     * see usage above this class
     */
    public static void formatDtoToRpcRequest(Object dto, GeneratedMessageV3.Builder rpgRequest) {
        Class tempClass = dto.getClass();
        Map<String, Object> responseFieldMap = new HashMap<>();
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(dto);
                        if (null != value) {
                            responseFieldMap.put(f.getName(), value);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }

        List<Descriptors.FieldDescriptor> buildFieldMap = rpgRequest.getDescriptorForType().getFields();
        for (Descriptors.FieldDescriptor f : buildFieldMap) {
            Object value = responseFieldMap.get(f.getJsonName());
            if (null != value) {
                String typeName = value.getClass().getName().toLowerCase();
                if (typeName.contains(f.getJavaType().name().toLowerCase())) {
                    rpgRequest.setField(f, value);
                } else if (typeName.contains("list") || typeName.contains("set")) {
                    rpgRequest.setField(f, JSON.toJSONString(value));
                }
            }
        }
    }

    /**
     * see usage above this class
     */
    public static <T> void formatDtoToRpcResponse(T t, GeneratedMessageV3.Builder builder) {
        Class tempClass = t.getClass();
        Map<String, Object> responseFieldMap = new HashMap<>();
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(t);
                        if (null != value) {
                            responseFieldMap.put(f.getName(), value);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }

        List<Descriptors.FieldDescriptor> buildFieldMap = builder.getDescriptorForType().getFields();
        for (Descriptors.FieldDescriptor f : buildFieldMap) {
            Object value = responseFieldMap.get(f.getJsonName());
            if (null != value) {
                String typeName = value.getClass().getName().toLowerCase();
                if (!typeName.contains("[") && typeName.contains(f.getJavaType().name().toLowerCase())) {
                    builder.setField(f, value);
                } else if (typeName.contains("[") || f.getJavaType().name().equals(Descriptors.FieldDescriptor.JavaType.STRING.name())) {
                    builder.setField(f, JSON.toJSONString(value));
                }
            }
        }
    }

    /**
     * see usage above this class
     */
    public static <T> T formatGrpcMessageToDto(GeneratedMessageV3 message, Class<T> t) {
        Map<String, Object> recvParamMap = new HashMap<>();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            recvParamMap.put(key.getJsonName(), entry.getValue());
        }
        T instance = null;
        try {
            instance = t.newInstance();
        } catch (Exception e) {
            //log.info("checkParam can't newInstance, class{},", t.getClass().getName());
            return null;
        }
        Class tempClass = t;
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    Object value = recvParamMap.get(f.getName());
                    if (null != value) {
                        try {
                            String typeName = f.getType().getName().toLowerCase();
                            if (typeName.contains(value.getClass().getName().toLowerCase())) {
                                f.set(instance, value);
                            } else {
                                if (typeName.contains("list") || typeName.contains("[")) {//List/Set/[]
//                                    DefaultJSONParser defaultJSONParser = new DefaultJSONParser(JSON.toJSONString(value));
                                    DefaultJSONParser defaultJSONParser = new DefaultJSONParser(value.toString());
//                                    Object destObj = CollectionDeserializer.instance.deserialze(defaultJSONParser, (Type) f.getType(), null);
//                                    f.set(instance, destObj);

                                    f.set(instance, defaultJSONParser.parseObject(f.getType()));
                                } else if (value.toString().contains("{")) {
                                    try {
                                        DefaultJSONParser defaultJSONParser = new DefaultJSONParser(JSON.toJSONString(value));
                                        Object destObj = MapDeserializer.instance.deserialze(defaultJSONParser, (Type) f.getType(), null);//Map
                                        f.set(instance, destObj);
                                        defaultJSONParser.close();
                                    } catch (Exception e) {//Object
                                        f.set(instance, JSON.parseObject(value.toString(), f.getType()));
                                    }
                                } else {//int<->Long
                                    if (typeName.equals("java.lang.object")) {
                                        f.set(instance, JSON.parseObject(value.toString(), f.getType()));
                                    } else {
                                        DefaultJSONParser defaultJSONParser = new DefaultJSONParser(JSON.toJSONString(value));
                                        f.set(instance, defaultJSONParser.parseObject(f.getType()));
                                        defaultJSONParser.close();
                                    }
                                }
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }
        return instance;
    }

    /**
     * see usage above this class
     */
    public static <T> T formatGrpcMessageToObjectDto(GeneratedMessageV3 message, Class<T> t, Class genericClass) {
        Map<String, Object> recvParamMap = new HashMap<>();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            recvParamMap.put(key.getJsonName(), entry.getValue());
        }
        T instance = null;
        try {
            instance = t.newInstance();
        } catch (Exception e) {
            //log.info("checkParam can't newInstance, class{},", t.getClass().getName());
            return null;
        }
        Class tempClass = t;
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    Object value = recvParamMap.get(f.getName());
                    if (null != value) {
                        try {
                            String typeName = f.getType().getName().toLowerCase();
                            if (typeName.equalsIgnoreCase(value.getClass().getName())) {
                                f.set(instance, value);
                            } else if (typeName.equalsIgnoreCase("java.lang.object")) {
                                f.set(instance, JSON.parseObject(value.toString(), genericClass));
                            } else {
                                f.set(instance, JSON.parseObject(value.toString(), f.getType()));
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }
        return instance;
    }

    /**
     * see usage above this class
     */
    public static <T> T formatGrpcMessageToListOrSetDto(GeneratedMessageV3 message, Class<T> t, Class collectorClass, Class genericClass) {
        Map<String, Object> recvParamMap = new HashMap<>();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            recvParamMap.put(key.getJsonName(), entry.getValue());
        }
        T instance = null;
        try {
            instance = t.newInstance();
        } catch (Exception e) {
            //log.info("checkParam can't newInstance, class{},", t.getClass().getName());
            return null;
        }
        Class tempClass = t;
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    Object value = recvParamMap.get(f.getName());
                    if (null != value) {
                        try {
                            String typeName = f.getType().getName().toLowerCase();
                            if (typeName.equalsIgnoreCase(value.getClass().getName())) {
                                f.set(instance, value);
                            } else if (typeName.equalsIgnoreCase("java.lang.object")) {
                                Method m = collectorClass.getDeclaredMethod("add", Object.class);
                                Object o = collectorClass.newInstance();
                                JSONArray array = JSONArray.parseArray(value.toString());
                                for (int i = 0; i < array.size(); i++) {
                                    m.invoke(o, array.getObject(i, genericClass));
                                }
                                f.set(instance, o);
                            } else {
                                f.set(instance, JSON.parseObject(value.toString(), f.getType()));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }
        return instance;
    }

    /**
     * see usage above this class
     */
    public static <T> T formatGrpcMessageToMapDto(GeneratedMessageV3 message, Class<T> t, Class keyClass, Class valueClass) {
        Map<String, Object> recvParamMap = new HashMap<>();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
            Descriptors.FieldDescriptor key = entry.getKey();
            recvParamMap.put(key.getJsonName(), entry.getValue());
        }
        T instance = null;
        try {
            instance = t.newInstance();
        } catch (Exception e) {
            //log.info("checkParam can't newInstance, class{},", t.getClass().getName());
            return null;
        }
        Class tempClass = t;
        while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
            Field[] fields = tempClass.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (Field f : fields) {
                    f.setAccessible(true);
                    Object value = recvParamMap.get(f.getName());
                    if (null != value) {
                        try {
                            String typeName = f.getType().getName().toLowerCase();
                            if (typeName.equalsIgnoreCase(value.getClass().getName())) {
                                f.set(instance, value);
                            } else if (typeName.equalsIgnoreCase("java.lang.object")) {
                                Map map = new HashMap();
                                Map<String, Object> innerMap = JSONObject.parseObject(value.toString()).getInnerMap();
                                Iterator<Map.Entry<String, Object>> it = innerMap.entrySet().iterator();
                                while (it.hasNext()) {
                                    Map.Entry<String, Object> e = it.next();
                                    map.put(JSON.parseObject(e.getKey(), keyClass), JSON.parseObject(e.getValue().toString(), valueClass));
                                }
                                f.set(instance, map);
                            } else {
                                f.set(instance, JSON.parseObject(value.toString(), f.getType()));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            tempClass = tempClass.getSuperclass(); //得到父类,然后赋给自己
        }
        return instance;
    }

    @Setter
    @Getter
    static class HDto {
        int id;
        String tag = "1";
        String content = "2";
        String pic = "3";
        String backgroundColor = "4";
        boolean following;
        int statusCount;
        int followerCount;
    }

    @Setter
    @Getter
    static class SDto {
        String name = "2";
        List<Result> results = Arrays.asList(new Result(), new Result());
        Map<Long, String> rtmap = new HashMap<Long, String>(){{put(5L, "5");}};

        @Setter
        @Getter
        static class Result {
            String url = "1";
            String title = "2";
            List<String> snippets = Arrays.asList("1", "2", "3");
        }
    }

    public static void main(String[] args) {

        HDto dto = new HDto();
        HotEventServiceOuterClass.HotEventRpcDto.Builder builder = HotEventServiceOuterClass.HotEventRpcDto.newBuilder();
        FormatUtil.formatDtoToRpcResponse(dto, builder);
        HotEventServiceOuterClass.HotEventRpcDto rdto = builder.build();
        //System.out.println(rdto);

        List<Descriptors.FieldDescriptor> fds = HotEventServiceOuterClass.SearchResponse.newBuilder().getDescriptorForType().getFields();
        //System.out.println(fds);

        HotEventServiceOuterClass.SearchResponse.Builder builder2 = HotEventServiceOuterClass.SearchResponse.newBuilder();

        SDto sDto = new SDto();
        FormatUtil.formatDtoToRpcResponse(sDto, builder2);
        HotEventServiceOuterClass.SearchResponse rsp = builder2.build();
        //System.out.println(rsp);


        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(HotEventServiceOuterClass.SearchResponse.Builder.class, "results");
        //System.out.println(pd);
        PropertyDescriptor pd2 = BeanUtils.getPropertyDescriptor(SDto.class, "results");
        //System.out.println(pd2);

        builder2.addResults(HotEventServiceOuterClass.Result.newBuilder().setTitle("1").setUrl("2").addAllSnippets(Arrays.asList("4","5")).build());
        Map<Long, String> rtMap = new HashMap<Long, String>(){{put(5L, "5");}};
        builder2.putAllRtmap(rtMap);
        builder2.setName("333");

        System.out.println(builder2.build().toString());


        builder2 = HotEventServiceOuterClass.SearchResponse.newBuilder();
        sDto = new SDto();
        GRpcMessageConverter.toGRpcMessage(sDto, builder2);
        System.out.println(builder2);

    }
}
