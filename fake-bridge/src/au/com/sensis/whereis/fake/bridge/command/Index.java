package au.com.sensis.whereis.fake.bridge.command;

import au.com.sensis.whereis.fake.bridge.AdbClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Index implements Executor {

    public static final String EMS_PREFIX_TOKEN = "ems-devx-web-vip.in.sensis.com.au/v2";
    public static final String EMS_ENVIRONMENT_PARAM = "ems_environment";
    public static final String EMS_HOST_PARAM = "ems_host";

    /**
	 * Input request: nothing
	 * Return result:  html interface
     * Ths is supposed to support "http://localhost:7100" browser request.
     * For that to work, in intellij, you must add web-src as a source folder
     * in the fake-bridge module Sources tab.
     * Also note that in the run/debug configuration, the working directory
     * must be stumbler/fake-bridge, so that the "routes" directory can be accessed.
	 */
	@Override
	public String execute(AdbClient adbClient, Map<String, String> parms) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            InputStream in = Index.class.getClassLoader().getResource("routegen.html").openStream();

            int inChar = -1;
            while ((inChar = in.read()) != -1) {
                bos.write(inChar);
            }
        } catch (IOException e) {
            throw new RuntimeException("error reading default html file!",e);
        } catch (NullPointerException e) {
            System.err.println("*ERROR* unable to open html file (" + e + ")");
            throw new RuntimeException("error reading default html file!",e);
        }

        String html = new String(bos.toByteArray());

        if (parms.containsKey("ems_environment")) {
            html = html.replaceAll(EMS_PREFIX_TOKEN,getEmsPrefix(parms.get(EMS_ENVIRONMENT_PARAM)));
        } else if (parms.containsKey("ems_host")) {
            html = html.replaceAll(EMS_PREFIX_TOKEN,parms.get(EMS_HOST_PARAM));
        }

        return html;
	}

    private String getEmsPrefix(String environment) {
        return Environment.getEnvironment(environment).getHostPrefix();
    }


    public enum Environment {
        DEV("dev"),
        TEST("test"),
        STAGE("stage"),
        PROD("prod");

        private String value = "";

        private Environment(String value) {
            this.value = value;
        }

        public static Environment getEnvironment(String value) {
            if (value.equalsIgnoreCase(DEV.value)) {
                return DEV;
            } else if (value.equalsIgnoreCase(TEST.value)) {
                return TEST;
            } else if (value.equalsIgnoreCase(TEST.value)) {
                return STAGE;
            } else if (value.equalsIgnoreCase(TEST.value)) {
                return PROD;
            } else {
                return PROD;
            }
        }

        public String getHostPrefix() {
            switch (this) {
                case DEV:
                    return "ems-devx-web-vip.in.sensis.com.au/v2";
                case TEST:
                    return "api-ems-test.ext.sensis.com.au/v2";
                case STAGE:
                    return "api-ems-stage.ext.sensis.com.au/v2";

                case PROD:
                default:
                    return "api.ems.sensis.com.au/v2";
            }
        }
    }
}
