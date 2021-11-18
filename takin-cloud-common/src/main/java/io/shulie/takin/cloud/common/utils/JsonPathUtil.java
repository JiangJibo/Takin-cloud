package io.shulie.takin.cloud.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.shulie.takin.ext.content.enums.NodeTypeEnum;
import io.shulie.takin.ext.content.script.ScriptNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author moriarty
 *
 * {@link JsonPath } 处理json字符串
 */
@Slf4j
public class JsonPathUtil {

    /**
     * 默认需要删除的节点，用以处理脚本的树状结构，删除无用的节点
     */
    public static final List<String> DEFAULT_REGEXPS = Collections.singletonList("$..props");

    /**
     * 默认匹配节点的key
     */
    public static final String DEFAULT_KEY = "xpathMd5";

    /**
     * 默认解析配置
     * jsonProvider 使用 {@link JacksonJsonProvider}
     * mappingProvider 使用 {@link JacksonMappingProvider}
     * options 默认总是返回{@link List}
     */
    public static final Configuration JACKSON_CONFIGURATION = Configuration.builder()
        .jsonProvider(new JacksonJsonProvider())
        .mappingProvider(new JacksonMappingProvider())
        .options(Option.ALWAYS_RETURN_LIST)
        .build();

    public static final TypeRef<List<ScriptNode>> SCRIPT_NODE_TYPE_REF = new TypeRef<List<ScriptNode>>() {};

    public static DocumentContext deleteByXpathMd5(String jsonString, List<String> md5Keys) {
        if (StringUtils.isBlank(jsonString)) {
            return null;
        }
        DocumentContext document = JsonPath.using(JACKSON_CONFIGURATION).parse(jsonString);
        if (CollectionUtils.isEmpty(md5Keys)) {
            return document;
        }
        md5Keys.stream()
            .filter(StringUtils::isNotBlank)
            .forEach(md5 -> {
                document.delete(JsonPath.compile("$..[?(@.xpathMd5=='" + md5 + "')]"));
            });
        return document;
    }

    /**
     * 删除json字符串中的节点
     *
     * @param jsonString 目标json字符串
     * @return json文档对象
     */
    public static DocumentContext deleteNodes(String jsonString) {
        return deleteNodes(jsonString, DEFAULT_REGEXPS);
    }

    /**
     * 删除json字符串中的节点，
     *
     * @param jsonString json字符串
     * @param regexps    需要删除的节点表达式
     *                   例如：删除所有的props：表达式为：$..props;
     *                   删除当前节点的name：表达式为 @.name
     *                   可以使用过滤条件进行过滤：
     *                   删除所有price大于10的节点：表达式为： $..[?(@.price > 10)]
     *                   过滤条件为字符串的表达式：$..[?(@.name='moriarty')]
     * @return json文档对象
     */
    public static DocumentContext deleteNodes(String jsonString, List<String> regexps) {
        if (StringUtils.isBlank(jsonString)) {
            return null;
        }
        DocumentContext context = null;
        try {
            context = JsonPath.parse(jsonString);
            DocumentContext finalContext = context;
            regexps.forEach(r -> finalContext.delete(JsonPath.compile(r)));
            return finalContext;
        } catch (Exception e) {
            log.error("json delete regex error!json={}", jsonString);
        }
        return context;
    }

    /**
     * 向json中添加节点
     *
     * @param context  json文档对象
     * @param key      需要添加节点的父节点名称
     * @param nodeMaps 需要添加的节点；外层Key为匹配父节点名称的value，内层map：key是json的key，value是json的value
     */
    public static void putNodesToJson(DocumentContext context, String key,
        Map<String, Map<String, Object>> nodeMaps) {
        if (Objects.isNull(context) || StringUtils.isBlank(key) || Objects.isNull(nodeMaps) || nodeMaps.size() <= 0) {
            return;
        }
        for (Map.Entry<String, Map<String, Object>> md5Entry : nodeMaps.entrySet()) {
            for (Map.Entry<String, Object> entry : md5Entry.getValue().entrySet()) {
                context.put(JsonPath.compile("$..[?(@." + key + "=='" + md5Entry.getKey() + "')]"), entry.getKey(),
                    entry.getValue());
            }
        }
    }

    public static void putNodesToJson(DocumentContext context, Map<String, Map<String, Object>> nodeMaps) {
        putNodesToJson(context, DEFAULT_KEY, nodeMaps);
    }

    public static String putNodesToJson(String targetJson, Map<String, Map<String, Object>> nodeMaps) {
        DocumentContext context = JsonPath.using(JACKSON_CONFIGURATION).parse(targetJson);
        putNodesToJson(context, nodeMaps);
        return context.jsonString();
    }

    public static List<ScriptNode> getCurrentNodeByMd5(String nodeTree, String xpathMd5) {
        return JsonPath.using(JACKSON_CONFIGURATION)
            .parse(nodeTree)
            .read("$..[?(@.xpathMd5=='" + xpathMd5 + "')]", SCRIPT_NODE_TYPE_REF);
    }

    /**
     * 根据xpathMd5值获取其子节点
     *
     * @param nodeTree 目标json
     * @param xpathMd5 xpathMd5值
     * @param nodeType {@link NodeTypeEnum} 节点类型
     * @return List<ScriptNode>
     */
    public static List<ScriptNode> getChildrenByMd5(String nodeTree, String xpathMd5, NodeTypeEnum nodeType) {
        if (StringUtils.isBlank(nodeTree)) {
            return null;
        }
        if (StringUtils.isBlank(xpathMd5)) {
            if (null != nodeType) {
                return JsonPath.using(JACKSON_CONFIGURATION)
                    .parse(nodeTree)
                    .read("$..children[?(@.type=='" + nodeType.name() + "')]", SCRIPT_NODE_TYPE_REF);
            }
            return JsonPath.using(JACKSON_CONFIGURATION)
                .parse(nodeTree)
                .read("$..children", SCRIPT_NODE_TYPE_REF);
        }
        Object read = JsonPath.read(nodeTree, "$..[?(@.xpathMd5=='" + xpathMd5 + "')]");
        if (Objects.isNull(read)) {
            return null;
        }
        return JsonPath
            .using(JACKSON_CONFIGURATION)
            .parse(read.toString())
            .read("$..children[?(@.type=='" + nodeType + "')]", SCRIPT_NODE_TYPE_REF);
    }

    /**
     * 根据xpathMd5值获取类型为控制器的子节点
     *
     * @param nodeTree 目标json
     * @param xpathMd5 xpathMd5值
     * @return List<ScriptNode>
     */
    public static List<ScriptNode> getChildControllers(String nodeTree, String xpathMd5) {
        return getChildrenByMd5(nodeTree, xpathMd5, NodeTypeEnum.CONTROLLER);
    }

    /**
     * 根据xpathMd5值获取类型为取样器的子节点
     *
     * @param nodeTree 目标json
     * @param xpathMd5 xpathMd5值
     * @return List<ScriptNode>
     */
    public static List<ScriptNode> getChildSamplers(String nodeTree, String xpathMd5) {
        return getChildrenByMd5(nodeTree, xpathMd5, NodeTypeEnum.SAMPLER);
    }

    public static void main(String[] args) {
        String json = "[\n"
            + "  {\n"
            + "    \"name\": \"TestPlan\",\n"
            + "    \"testName\": \"Test Plan\",\n"
            + "    \"md5\": \"5e2a170b894b9b7346c858aa1b609038\",\n"
            + "    \"type\": \"TEST_PLAN\",\n"
            + "    \"xpath\": \"/jmeterTestPlan/hashTree/TestPlan\",\n"
            + "    \"xpathMd5\": \"0f1a197a2040e645dcdb4dfff8a3f960\",\n"
            + "    \"children\": [\n"
            + "      {\n"
            + "        \"name\": \"ThreadGroup\",\n"
            + "        \"testName\": \"线程组\",\n"
            + "        \"md5\": \"d0bba04950f1e3e68e7d97d614b0b5b9\",\n"
            + "        \"type\": \"THREAD_GROUP\",\n"
            + "        \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/ThreadGroup[1]\",\n"
            + "        \"xpathMd5\": \"cec45d27c5e20cca29526c54b4c9ad34\",\n"
            + "        \"props\": {\n"
            + "          \"ThreadGroup.on_sample_error\": \"continue\",\n"
            + "          \"ThreadGroup.scheduler\": \"false\",\n"
            + "          \"ThreadGroup.num_threads\": \"1\",\n"
            + "          \"ThreadGroup.same_user_on_next_iteration\": \"true\",\n"
            + "          \"ThreadGroup.ramp_time\": \"1\",\n"
            + "          \"ThreadGroup.delay\": \"\",\n"
            + "          \"ThreadGroup.duration\": \"10\"\n"
            + "        },\n"
            + "        \"children\": [\n"
            + "          {\n"
            + "            \"name\": \"TransactionController\",\n"
            + "            \"testName\": \"事务控制器\",\n"
            + "            \"md5\": \"f4409c957e9e7129a0c1856c1ff2231b\",\n"
            + "            \"type\": \"CONTROLLER\",\n"
            + "            \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/TransactionController\",\n"
            + "            \"xpathMd5\": \"99f81d69dc53260e8b0e85aa3d296b9c\",\n"
            + "            \"children\": [\n"
            + "              {\n"
            + "                \"name\": \"HTTPSamplerProxy\",\n"
            + "                \"testName\": \"【Http】1.创建订单\",\n"
            + "                \"md5\": \"159036bb8a7ad4451f55b16772d16305\",\n"
            + "                \"type\": \"SAMPLER\",\n"
            + "                \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3"
            + "]/HTTPSamplerProxy\",\n"
            + "                \"xpathMd5\": \"0dc5078261a2860acc8e530a46d17f13\",\n"
            + "                \"props\": {\n"
            + "                  \"HTTPSampler.protocol\": \"http\",\n"
            + "                  \"HTTPSampler.response_timeout\": \"\",\n"
            + "                  \"HTTPSampler.port\": \"80\",\n"
            + "                  \"HTTPSampler.implementation\": \"HttpClient4\",\n"
            + "                  \"HTTPSampler.connect_timeout\": \"15000\",\n"
            + "                  \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                  \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                  \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                  \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                  \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                  \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                  \"HTTPSampler.domain\": \"ots-test05.shein.com\",\n"
            + "                  \"HTTPSampler.method\": \"POST\",\n"
            + "                  \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                  \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                  \"HTTPSampler.contentEncoding\": \"UTF-8\",\n"
            + "                  \"HTTPSampler.path\": \"/order/createOrder.do\"\n"
            + "                },\n"
            + "                \"identification\": \"http#/order/createOrder.do#POST\",\n"
            + "                \"children\": []\n"
            + "              },\n"
            + "              {\n"
            + "                \"name\": \"IfController\",\n"
            + "                \"testName\": \"【If】code非0，终止后续流程\",\n"
            + "                \"md5\": \"bd6b15329a6676361854d3fa9b527e0e\",\n"
            + "                \"type\": \"CONTROLLER\",\n"
            + "                \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/IfController\",\n"
            + "                \"xpathMd5\": \"4fc62872a7161c458c6a075fdefa73ca\",\n"
            + "                \"children\": [\n"
            + "                  {\n"
            + "                    \"name\": \"HTTPSamplerProxy\",\n"
            + "                    \"testName\": \"【Http】2.接收支付中心返回结果\",\n"
            + "                    \"md5\": \"4d8edbf3c44644e70a53b1d52a26bad0\",\n"
            + "                    \"type\": \"SAMPLER\",\n"
            + "                    \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2"
            + "]/HTTPSamplerProxy\",\n"
            + "                    \"xpathMd5\": \"5acad73dae0387932ffe046fc123254e\",\n"
            + "                    \"props\": {\n"
            + "                      \"HTTPSampler.protocol\": \"http\",\n"
            + "                      \"HTTPSampler.response_timeout\": \"\",\n"
            + "                      \"HTTPSampler.port\": \"80\",\n"
            + "                      \"HTTPSampler.implementation\": \"HttpClient4\",\n"
            + "                      \"HTTPSampler.connect_timeout\": \"15000\",\n"
            + "                      \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                      \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                      \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                      \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                      \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                      \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                      \"HTTPSampler.domain\": \"ots-test05.shein.com\",\n"
            + "                      \"HTTPSampler.method\": \"POST\",\n"
            + "                      \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                      \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                      \"HTTPSampler.contentEncoding\": \"UTF-8\",\n"
            + "                      \"HTTPSampler.path\": \"/payment/acceptPayResult.do\"\n"
            + "                    },\n"
            + "                    \"identification\": \"http#/payment/acceptPayResult.do#POST\",\n"
            + "                    \"children\": []\n"
            + "                  },\n"
            + "                  {\n"
            + "                    \"name\": \"IfController\",\n"
            + "                    \"testName\": \"【If】code非0，终止后续流程\",\n"
            + "                    \"md5\": \"c7807ed465ddaa0259fcb54bbf5493fe\",\n"
            + "                    \"type\": \"CONTROLLER\",\n"
            + "                    \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2"
            + "]/IfController\",\n"
            + "                    \"xpathMd5\": \"09b6fa0c52c59c4fbda7508d27c1ff5a\",\n"
            + "                    \"children\": [\n"
            + "                      {\n"
            + "                        \"name\": \"HTTPSamplerProxy\",\n"
            + "                        \"testName\": \"【Http】3.订单取消作废标记\",\n"
            + "                        \"md5\": \"827bfecd716d8a4798d117a5c796b186\",\n"
            + "                        \"type\": \"SAMPLER\",\n"
            + "                        \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3"
            + "]/hashTree[2]/hashTree[2]/HTTPSamplerProxy\",\n"
            + "                        \"xpathMd5\": \"fff9e9852aeede646d6df08e9d332897\",\n"
            + "                        \"props\": {\n"
            + "                          \"HTTPSampler.protocol\": \"http\",\n"
            + "                          \"HTTPSampler.response_timeout\": \"\",\n"
            + "                          \"HTTPSampler.port\": \"80\",\n"
            + "                          \"HTTPSampler.implementation\": \"HttpClient4\",\n"
            + "                          \"HTTPSampler.connect_timeout\": \"15000\",\n"
            + "                          \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                          \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                          \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                          \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                          \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                          \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                          \"HTTPSampler.domain\": \"ots-test05.shein.com\",\n"
            + "                          \"HTTPSampler.method\": \"POST\",\n"
            + "                          \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                          \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                          \"HTTPSampler.contentEncoding\": \"UTF-8\",\n"
            + "                          \"HTTPSampler.path\": \"/order/batchMarkCancelMarkOrder.do\"\n"
            + "                        },\n"
            + "                        \"identification\": \"http#/order/batchMarkCancelMarkOrder.do#POST\",\n"
            + "                        \"children\": []\n"
            + "                      },\n"
            + "                      {\n"
            + "                        \"name\": \"IfController\",\n"
            + "                        \"testName\": \"【If】code非0，终止后续流程\",\n"
            + "                        \"md5\": \"69965477576732ed09e475e0512296d8\",\n"
            + "                        \"type\": \"CONTROLLER\",\n"
            + "                        \"xpath\": \"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3"
            + "]/hashTree[2]/hashTree[2]/IfController\",\n"
            + "                        \"xpathMd5\": \"79ed27c6b3472714af1a09a9842b9114\",\n"
            + "                        \"children\": [\n"
            + "                          {\n"
            + "                            \"name\": \"HTTPSamplerProxy\",\n"
            + "                            \"testName\": \"【Http】4.接收风控结果\",\n"
            + "                            \"md5\": \"d6ee0a5999ef59f1577385a86308f901\",\n"
            + "                            \"type\": \"SAMPLER\",\n"
            + "                            \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/HTTPSamplerProxy[1]\",\n"
            + "                            \"xpathMd5\": \"d53759e4d7aadda84e6a1b2c517eddf8\",\n"
            + "                            \"props\": {\n"
            + "                              \"HTTPSampler.protocol\": \"http\",\n"
            + "                              \"HTTPSampler.response_timeout\": \"\",\n"
            + "                              \"HTTPSampler.port\": \"80\",\n"
            + "                              \"HTTPSampler.implementation\": \"HttpClient4\",\n"
            + "                              \"HTTPSampler.connect_timeout\": \"15000\",\n"
            + "                              \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                              \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                              \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                              \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                              \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                              \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                              \"HTTPSampler.domain\": \"ots-test05.shein.com\",\n"
            + "                              \"HTTPSampler.method\": \"POST\",\n"
            + "                              \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                              \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                              \"HTTPSampler.contentEncoding\": \"UTF-8\",\n"
            + "                              \"HTTPSampler.path\": \"/order/batchReceiveMallOrder.do\"\n"
            + "                            },\n"
            + "                            \"identification\": \"http#/order/batchReceiveMallOrder.do#POST\",\n"
            + "                            \"children\": []\n"
            + "                          },\n"
            + "                          {\n"
            + "                            \"name\": \"HTTPSamplerProxy\",\n"
            + "                            \"testName\": \"手动同步ops\",\n"
            + "                            \"md5\": \"06da765020c07add53f84ea3f65bdd04\",\n"
            + "                            \"type\": \"SAMPLER\",\n"
            + "                            \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/HTTPSamplerProxy[2]\",\n"
            + "                            \"xpathMd5\": \"f0590072b904e00ee4a85987baa5b1f1\",\n"
            + "                            \"props\": {\n"
            + "                              \"HTTPSampler.protocol\": \"http\",\n"
            + "                              \"HTTPSampler.response_timeout\": \"\",\n"
            + "                              \"HTTPSampler.port\": \"80\",\n"
            + "                              \"HTTPSampler.implementation\": \"HttpClient4\",\n"
            + "                              \"HTTPSampler.connect_timeout\": \"15000\",\n"
            + "                              \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                              \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                              \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                              \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                              \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                              \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                              \"HTTPSampler.domain\": \"ots-test05.shein.com\",\n"
            + "                              \"HTTPSampler.method\": \"POST\",\n"
            + "                              \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                              \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                              \"HTTPSampler.contentEncoding\": \"UTF-8\",\n"
            + "                              \"HTTPSampler.path\": \"/sync/reSyncOrderInfoToOms.do\"\n"
            + "                            },\n"
            + "                            \"identification\": \"http#/sync/reSyncOrderInfoToOms.do#POST\",\n"
            + "                            \"children\": []\n"
            + "                          },\n"
            + "                          {\n"
            + "                            \"name\": \"IfController\",\n"
            + "                            \"testName\": \"【If】审核失败，不进行ops履约\",\n"
            + "                            \"md5\": \"a650338b8a3cc664b2227c73977dd4a7\",\n"
            + "                            \"type\": \"CONTROLLER\",\n"
            + "                            \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/IfController\",\n"
            + "                            \"xpathMd5\": \"db65854f3f8b92d60658fbdbde490d38\",\n"
            + "                            \"children\": [\n"
            + "                              {\n"
            + "                                \"name\": \"HTTPSamplerProxy\",\n"
            + "                                \"testName\": \"【Http】5.获取订单数据\",\n"
            + "                                \"md5\": \"f011df43fedd545a0965bbfa87ca035c\",\n"
            + "                                \"type\": \"SAMPLER\",\n"
            + "                                \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/HTTPSamplerProxy\",\n"
            + "                                \"xpathMd5\": \"8664e7ceb1c7a573899b664ab986ad95\",\n"
            + "                                \"props\": {\n"
            + "                                  \"HTTPSampler.protocol\": \"http\",\n"
            + "                                  \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                  \"HTTPSampler.port\": \"\",\n"
            + "                                  \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                  \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                  \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                  \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                  \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                  \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                  \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                  \"HTTPSampler.domain\": \"ops-test05.shein.com\",\n"
            + "                                  \"HTTPSampler.method\": \"POST\",\n"
            + "                                  \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                  \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                  \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                  \"HTTPSampler.path\": \"/outer/dataSyncQueryForOqs\"\n"
            + "                                },\n"
            + "                                \"identification\": \"http#/outer/dataSyncQueryForOqs#POST\",\n"
            + "                                \"children\": []\n"
            + "                              },\n"
            + "                              {\n"
            + "                                \"name\": \"IfController\",\n"
            + "                                \"testName\": \"【If】查询不到符合数据，不进行履约处理\",\n"
            + "                                \"md5\": \"332cc5d3d0efc21d0d0093bbb8dd65ab\",\n"
            + "                                \"type\": \"CONTROLLER\",\n"
            + "                                \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/IfController\",\n"
            + "                                \"xpathMd5\": \"2a572833238564890266e300c9c5d289\",\n"
            + "                                \"children\": [\n"
            + "                                  {\n"
            + "                                    \"name\": \"HTTPSamplerProxy\",\n"
            + "                                    \"testName\": \"【Http】生成包裹号数据\",\n"
            + "                                    \"md5\": \"163100ad37bec2023251af50b7f2aa23\",\n"
            + "                                    \"type\": \"SAMPLER\",\n"
            + "                                    \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/HTTPSamplerProxy[1]\",\n"
            + "                                    \"xpathMd5\": \"0d5514a0eeabda057a99a40c6ef18d88\",\n"
            + "                                    \"props\": {\n"
            + "                                      \"HTTPSampler.protocol\": \"http\",\n"
            + "                                      \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                      \"HTTPSampler.port\": \"\",\n"
            + "                                      \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                      \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                      \"TestPlan.comments\": \"模拟mq推送包裹信息\",\n"
            + "                                      \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                      \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                      \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                      \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                      \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                      \"HTTPSampler.domain\": \"mq-test01.dev.sheincorp.cn\",\n"
            + "                                      \"HTTPSampler.method\": \"POST\",\n"
            + "                                      \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                      \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                      \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                      \"HTTPSampler.path\": \"/api/exchanges/ops_test01/amq"
            + ".default/publish\"\n"
            + "                                    },\n"
            + "                                    \"identification\": \"http#/api/exchanges/ops_test01/amq"
            + ".default/publish#POST\",\n"
            + "                                    \"children\": []\n"
            + "                                  },\n"
            + "                                  {\n"
            + "                                    \"name\": \"IfController\",\n"
            + "                                    \"testName\": \"【If】推送失败，不进行wms已打印状态同步\",\n"
            + "                                    \"md5\": \"854d5f6fa9a3d9d7068af4039be7c865\",\n"
            + "                                    \"type\": \"CONTROLLER\",\n"
            + "                                    \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/IfController\",\n"
            + "                                    \"xpathMd5\": \"28285ffd12bdd6f1cf95201471416c43\",\n"
            + "                                    \"children\": [\n"
            + "                                      {\n"
            + "                                        \"name\": \"HTTPSamplerProxy\",\n"
            + "                                        \"testName\": \"【Http】等待出仓已打印\",\n"
            + "                                        \"md5\": \"af5d00ff3a37fb316715d484e675dd70\",\n"
            + "                                        \"type\": \"SAMPLER\",\n"
            + "                                        \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/hashTree[5]/HTTPSamplerProxy[1]\",\n"
            + "                                        \"xpathMd5\": \"373cf213cb8b2328d7fc4b66e885c5d3\",\n"
            + "                                        \"props\": {\n"
            + "                                          \"HTTPSampler.protocol\": \"http\",\n"
            + "                                          \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.port\": \"\",\n"
            + "                                          \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                          \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                          \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                          \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                          \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                          \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                          \"HTTPSampler.domain\": \"mq-test01.dev.sheincorp.cn\",\n"
            + "                                          \"HTTPSampler.method\": \"POST\",\n"
            + "                                          \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                          \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                          \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                          \"HTTPSampler.path\": \"/api/exchanges/ops_test01/amq"
            + ".default/publish\"\n"
            + "                                        },\n"
            + "                                        \"identification\": \"http#/api/exchanges/ops_test01/amq"
            + ".default/publish#POST\",\n"
            + "                                        \"children\": []\n"
            + "                                      },\n"
            + "                                      {\n"
            + "                                        \"name\": \"HTTPSamplerProxy\",\n"
            + "                                        \"testName\": \"【Http】等待发货\",\n"
            + "                                        \"md5\": \"c2cc445d162ac2ce4f9d45658c778b44\",\n"
            + "                                        \"type\": \"SAMPLER\",\n"
            + "                                        \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/hashTree[5]/HTTPSamplerProxy[2]\",\n"
            + "                                        \"xpathMd5\": \"3fa30c927e0e402d1b5c8cb81894a55b\",\n"
            + "                                        \"props\": {\n"
            + "                                          \"HTTPSampler.protocol\": \"http\",\n"
            + "                                          \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.port\": \"\",\n"
            + "                                          \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                          \"TestPlan.comments\": \"模拟mq推送包裹信息\",\n"
            + "                                          \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                          \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                          \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                          \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                          \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                          \"HTTPSampler.domain\": \"mq-test01.dev.sheincorp.cn\",\n"
            + "                                          \"HTTPSampler.method\": \"POST\",\n"
            + "                                          \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                          \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                          \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                          \"HTTPSampler.path\": \"/api/exchanges/ops_test01/amq"
            + ".default/publish\"\n"
            + "                                        },\n"
            + "                                        \"identification\": \"http#/api/exchanges/ops_test01/amq"
            + ".default/publish#POST\",\n"
            + "                                        \"children\": []\n"
            + "                                      },\n"
            + "                                      {\n"
            + "                                        \"name\": \"HTTPSamplerProxy\",\n"
            + "                                        \"testName\": \"【Http】发货中\",\n"
            + "                                        \"md5\": \"a4daf90842b82b304e2632446f75c3a4\",\n"
            + "                                        \"type\": \"SAMPLER\",\n"
            + "                                        \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/hashTree[5]/HTTPSamplerProxy[3]\",\n"
            + "                                        \"xpathMd5\": \"af28da1555f1d9d6bf7d79f0e65b00dc\",\n"
            + "                                        \"props\": {\n"
            + "                                          \"HTTPSampler.protocol\": \"http\",\n"
            + "                                          \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.port\": \"\",\n"
            + "                                          \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                          \"TestPlan.comments\": \"模拟mq推送包裹信息\",\n"
            + "                                          \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                          \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                          \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                          \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                          \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                          \"HTTPSampler.domain\": \"mq-test01.dev.sheincorp.cn\",\n"
            + "                                          \"HTTPSampler.method\": \"POST\",\n"
            + "                                          \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                          \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                          \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                          \"HTTPSampler.path\": \"/api/exchanges/ops_test01/amq"
            + ".default/publish\"\n"
            + "                                        },\n"
            + "                                        \"identification\": \"http#/api/exchanges/ops_test01/amq"
            + ".default/publish#POST\",\n"
            + "                                        \"children\": []\n"
            + "                                      },\n"
            + "                                      {\n"
            + "                                        \"name\": \"HTTPSamplerProxy\",\n"
            + "                                        \"testName\": \"【Http】发货\",\n"
            + "                                        \"md5\": \"ce7b28d2299956b2e5ae3fc1621c2f70\",\n"
            + "                                        \"type\": \"SAMPLER\",\n"
            + "                                        \"xpath\": "
            + "\"/jmeterTestPlan/hashTree/hashTree/hashTree[1]/hashTree[3]/hashTree[2]/hashTree[2]/hashTree[2"
            + "]/hashTree[3]/hashTree[4]/hashTree[5]/HTTPSamplerProxy[4]\",\n"
            + "                                        \"xpathMd5\": \"be63a722894e7a915975a2c857ce2ac7\",\n"
            + "                                        \"props\": {\n"
            + "                                          \"HTTPSampler.protocol\": \"http\",\n"
            + "                                          \"HTTPSampler.response_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.port\": \"\",\n"
            + "                                          \"HTTPSampler.connect_timeout\": \"\",\n"
            + "                                          \"HTTPSampler.concurrentPool\": \"6\",\n"
            + "                                          \"TestPlan.comments\": \"模拟mq推送包裹信息\",\n"
            + "                                          \"HTTPSampler.use_keepalive\": \"true\",\n"
            + "                                          \"HTTPSampler.auto_redirects\": \"false\",\n"
            + "                                          \"HTTPSampler.DO_MULTIPART_POST\": \"false\",\n"
            + "                                          \"HTTPSampler.embedded_url_exclude_re\": \"\",\n"
            + "                                          \"HTTPSampler.follow_redirects\": \"true\",\n"
            + "                                          \"HTTPSampler.domain\": \"mq-test01.dev.sheincorp.cn\",\n"
            + "                                          \"HTTPSampler.method\": \"POST\",\n"
            + "                                          \"HTTPSampler.postBodyRaw\": \"true\",\n"
            + "                                          \"HTTPSampler.embedded_url_re\": \"\",\n"
            + "                                          \"HTTPSampler.contentEncoding\": \"\",\n"
            + "                                          \"HTTPSampler.path\": \"/api/exchanges/ops_test01/amq"
            + ".default/publish\"\n"
            + "                                        },\n"
            + "                                        \"identification\": \"http#/api/exchanges/ops_test01/amq"
            + ".default/publish#POST\",\n"
            + "                                        \"children\": []\n"
            + "                                      }\n"
            + "                                    ]\n"
            + "                                  }\n"
            + "                                ]\n"
            + "                              }\n"
            + "                            ]\n"
            + "                          }\n"
            + "                        ]\n"
            + "                      }\n"
            + "                    ]\n"
            + "                  }\n"
            + "                ]\n"
            + "              }\n"
            + "            ]\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    ]\n"
            + "  }\n"
            + "]";

        DocumentContext context = JsonPathUtil.deleteByXpathMd5(json, Arrays.asList("d53759e4d7aadda84e6a1b2c517eddf8","db65854f3f8b92d60658fbdbde490d38","79ed27c6b3472714af1a09a9842b9114"));
        //DocumentContext context1 = JsonPathUtil.deleteNodes(context.jsonString());
        //List<ScriptNode> childSamplers = JsonPathUtil.getChildSamplers(json,
        //    "159036bb8a7ad4451f55b16772d16305");
        //childSamplers.forEach(System.out::println);
        System.out.println(context.jsonString());
    }
}
