import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import com.webank.ai.fate.serving.adapter.processing.CommonPreProcessing;
import com.webank.ai.fate.serving.bean.PreProcessingResult;
import com.webank.ai.fate.serving.core.bean.Configuration;
import com.webank.ai.fate.serving.federatedml.DSLParser;

/** Bounded, synthetic-only compatibility/regression harness; no real config/services or resource exhaustion. */
public final class JsonCompatibilityRegression {
  interface Case { void run() throws Exception; }
  static int ordinaryPassed, guardPassed, ordinaryFailed, guardFailed;
  static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
  static void run(String name, boolean guard, Case c) {
    try { c.run(); if (guard) guardPassed++; else ordinaryPassed++; System.out.println("PASS "+name); }
    catch (Throwable e) { if (guard) guardFailed++; else ordinaryFailed++; System.out.println("FAIL "+name+" "+e.getClass().getName()+" "+e.getMessage()); }
  }
  static String nested(int n) { String x="{\"a\":1}"; for(int i=0;i<n;i++)x="{\"a\":1;\t\u0000"+x+":1}"; return x; }
  static void rejected(String s) { try { new JSONObject(s); throw new AssertionError("accepted malformed input"); } catch(JSONException expected) {} }
  static String dsl() { return "{\"components\":{\"reader_0\":{\"CodePath\":\"unit/Reader\",\"input\":{\"data\":{\"data\":[\"args.train\"]}}},\"transform_0\":{\"CodePath\":\"unit/Transform\",\"input\":{\"data\":{\"data\":[\"reader_0.data\"]}}}}}"; }
  public static void main(String[] args) throws Exception {
    check(args.length == 1, "provide one new disposable synthetic-config directory");
    Path supplied = Paths.get(args[0]);
    if (Files.exists(supplied)) {
      check(Files.isDirectory(supplied), "synthetic directory required");
      try (java.util.stream.Stream<Path> entries = Files.list(supplied)) {
        check(!entries.findAny().isPresent(), "refuse to overwrite a nonempty directory");
      }
    }
    System.out.println("java="+System.getProperty("java.runtime.version")+" JSON="+JSONObject.class.getProtectionDomain().getCodeSource().getLocation());
    run("objects-arrays-unicode-number-access",false,()->{JSONObject o=new JSONObject("{\"u\":\"合成\\n\\u0041\",\"n\":1.5,\"i\":7,\"a\":[\"x\",true]}");check(o.getString("u").equals("合成\nA"),"Unicode");check(o.getInt("i")==7 && ((Number)o.get("n")).doubleValue()==1.5,"numbers");check(o.getJSONArray("a").getString(0).equals("x")&&o.getJSONArray("a").getBoolean(1),"array");});
    run("put-null-remove-roundtrip",false,()->{JSONObject o=new JSONObject();o.put("x",JSONObject.NULL);o.put("child",new JSONObject().put("k","v"));check(o.has("x")&&o.isNull("x"),"null");JSONObject round=new JSONObject(o.toString());check(round.getJSONObject("child").getString("k").equals("v"),"roundtrip");round.remove("x");check(!round.has("x"),"remove");});
    run("ordinary-bounded-nested-json",false,()->{JSONObject o=new JSONObject("{\"a\":{\"b\":[{\"c\":\"value\"}]}}");check(o.getJSONObject("a").getJSONArray("b").getJSONObject(0).getString("c").equals("value"),"nested");});
    run("actual-common-preprocessing",false,()->{PreProcessingResult r=new CommonPreProcessing().getResult(null,"{\"synthetic\":[1,2],\"label\":\"ok\"}");check(r.getFeatureIds().isEmpty()&&r.getProcessingResult().isEmpty(),"unchanged empty maps");});
    run("actual-dsl-toposort",false,()->{ArrayList<String> rank=new ArrayList<>();new DSLParser().topoSort(new JSONObject(dsl()).getJSONObject("components"),rank);check(rank.equals(Arrays.asList("reader_0","transform_0")),"topological chain");});
    run("actual-dsl-parse-mappings",false,()->{DSLParser d=new DSLParser();check(d.parseDagFromDSL(dsl())==0,"status");check(d.getAllComponent().equals(Arrays.asList("reader_0","transform_0")),"rank");check(d.getComponentModuleMap().get("reader_0").equals("Reader")&&d.getComponentModuleMap().get("transform_0").equals("Transform"),"module map");check(d.getUpInputComponents(0).equals(new HashSet<Integer>(Arrays.asList(-1)))&&d.getUpInputComponents(1).equals(new HashSet<Integer>(Arrays.asList(0))),"upstream ids");});
    run("actual-config-loader-synthetic-only",false,()->{Path dir=Paths.get(args[0]);Files.createDirectories(dir.resolve("adapter_conf"));Files.write(dir.resolve("unit.properties"),"unit.count=3\n".getBytes(StandardCharsets.UTF_8));Files.write(dir.resolve("adapter_conf/unit.json"),"{\"label\":\"合成\",\"threshold\":1.25}".getBytes(StandardCharsets.UTF_8));Configuration c=new Configuration(dir.resolve("unit.properties").toString());check(c.load()==0,"load");JSONObject o=Configuration.getAdapterJsonConfig("unit.json");check(o.getString("label").equals("合成")&&((Number)o.get("threshold")).doubleValue()==1.25,"JSON config");check(Configuration.getPropertyInt("unit.count")==3,"properties");});
    run("ordinary-malformed-json-controls",false,()->{rejected("{\"a\":");rejected("{\"a\":1,\"a\":2}");});
    run("loaded-exact-patched-artifact",true,()->{Properties p=new Properties();try(InputStream in=JSONObject.class.getResourceAsStream("/META-INF/maven/org.json/json/pom.properties")){check(in!=null,"embedded pom.properties");p.load(in);}check("20231013".equals(p.getProperty("version")),"expected20231013 got "+p.getProperty("version"));});
    run("embedded-null-small-depth2-rejected",true,()->{String s=nested(2);check(s.length()<80,"bounded");rejected(s);});
    run("array-key-small-input-rejected",true,()->{rejected("{[1]:1}");});
    System.out.printf("RESULT normal_pass=%d normal_fail=%d guard_pass=%d guard_fail=%d%n",ordinaryPassed,ordinaryFailed,guardPassed,guardFailed);
    if(ordinaryFailed+guardFailed>0)System.exit(1);
  }
}
