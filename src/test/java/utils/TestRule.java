package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.ChartLocation;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.core.backend.TestCaseState;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.sikuli.basics.Settings;
import org.sikuli.script.App;
import org.sikuli.script.ImagePath;
import org.sikuli.script.Screen;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestRule extends TestWatcher {

    private static WebDriver driver;
    private static Screen sikuli;
    private static App sikuliApp;
    private static ExtentReports extentReport;
    private static Scenario scenario;
    private static ExtentTest extentTest;
    private static String activeAutomation;
    public static String browser;
    public TestRule() {
        super();
    }

    @Override
    protected void starting(Description description) {
        super.starting(description);

        new File("target/report").mkdir();
        new File("target/report/pdf").mkdir();
        new File("target/temp").mkdir();
        Utils.deleteAllFilesInFolder("target/temp");

        //EXTENTREPORT
        File imgFolder = new File("target/report/html/img");
        File[] contents = imgFolder.listFiles();
        if (contents != null) {
            for (File f : contents) {
                f.delete();
            }
        }
        new File("target/report/html").mkdir();
        new File("target/report/html/img").mkdir();
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("target/report/html/" + description.getDisplayName().replace("tests.", "") + ".html");
        //htmlReporter.config().setEncoding("ISO-8859-1");
        htmlReporter.config().setTheme(Theme.DARK);
        htmlReporter.config().setEncoding("utf-8");
        htmlReporter.config().setTestViewChartLocation(ChartLocation.BOTTOM);
        htmlReporter.config().setTimeStampFormat("dd MMM yyyy, HH:mm:ss");
        htmlReporter.config().setCSS(".node.level-1  ul{ display:none;} .node.level-1.active ul{display:block;}  .card-panel.environment  th:first-child{ width:30%;}");
        htmlReporter.config().setJS("$(window).off(\"keydown\");");
        extentReport = new ExtentReports();
        extentReport.attachReporter(htmlReporter);
        //Utils.addAllTestPropertiesToExtentReport(extentReport);
        extentReport.setSystemInfo("os.name", System.getProperty("os.name"));
    }

    @Before
    public void beforeCenario(Scenario scenario) {
        TestRule.scenario = scenario;

        System.out.println("Thread ID - " + Thread.currentThread().getId());

        //START EXTENTTEST
        extentTest = extentReport.createTest("Cenario: " + scenario.getName(), scenario.getName());
        extentTest.assignCategory("feature:" + scenario.getId().replaceAll(";.*", ""));
        Collection<String> tags = scenario.getSourceTagNames();
        for (String tag : tags) {
            extentTest.assignCategory(tag);
        }

        System.out.println("Cenario: " + scenario.getName());
    }

    @After
    public void afterCenario() throws IOException, InterruptedException, SQLException, ClassNotFoundException {
        if (scenario.isFailed()) {
            if (driver != null || sikuliApp != null) {
                BasePage basePage = new BasePage();
                basePage.logPrintFail("The test is failed");

                Throwable throwable = logError(scenario);
                extentTest.fail(throwable);
                String errorMessage = throwable.getMessage();
                if (errorMessage != null) {
                    if (errorMessage.contains("FindFailed")) {
                        try {
                            String findFailedImage = Utils.substringRegexGroup1("FindFailed: (.*): ", errorMessage);
                            FileUtils.copyFile(new File(ImagePath.find(findFailedImage).getPath()), new File("target/report/html/img/" + findFailedImage));
                            extentTest.info("FindFailed: " + findFailedImage, MediaEntityBuilder.createScreenCaptureFromPath("img/" + findFailedImage).build());
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        //FINISH EXTENT REPORT
        extentReport.flush();

        //FINISH DRIVER
        if (driver != null) {
            driver.quit();
            driver = null;
        }

        //FINISH SIKULI
        if (sikuliApp != null) {
            sikuli.keyUp();
            sikuli.mouseUp();
            sikuliApp.close();
            sikuliApp = null;
        }
    }

    private Throwable logError(Scenario scenario) {
        Field field = FieldUtils.getField(Scenario.class, "delegate", true);
        Method getError = null;
        try {
            final TestCaseState testCase = (TestCaseState) field.get(scenario);
            if (getError == null) {
                getError = MethodUtils.getMatchingMethod(testCase.getClass(), "getError");
                getError.setAccessible(true);
            }
            return (Throwable) getError.invoke(testCase);
        } catch (Exception e) {
            System.err.println("error receiving exception" + e);
        }
        return null;
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
    }

    public static WebDriver getDriver() {
        return driver;
    }

    public static ExtentTest getExtentTest() {
        return extentTest;
    }

    public static Scenario getScenario() {
        return scenario;
    }

    public static Screen getSikuli() {
        return sikuli;
    }

    public static App getSikuliApp() {
        return sikuliApp;
    }

    public static void setSikuliApp(App app) {
        sikuliApp = app;
    }

    public static ExtentReports getExtentReports() {
        return extentReport;
    }

    public static String getActiveAutomation() {
        return activeAutomation;
    }

    public static void openApplication(String application, String url) {
        browser = application;
        switch (application) {
            case "chrome":
                openApplicationChrome(url);
                break;
            case "ie":
                openApplicationIE(url);
                break;
            case "firefox":
                openApplicationFirefox(url);
                break;
            case "edge":
                openApplicationEdge(url);
                break;
            case "sikuli":
                openApplicationSikuli(url);
                break;
            case "chromemobile":
                openApplicationChromeMobile(url);
                break;
            default:
                System.err.print("Property test.application: " + Utils.getTestProperty("test.application") + " nao encontrada.");
                break;
        }
    }




    public static void openApplicationChrome(String url) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension d = tk.getScreenSize();
        System.out.println("Screen width = " + d.width);
        System.out.println("Screen height = " + d.height);
        activeAutomation = "chrome";
        String downloadFilepath = System.getProperty("user.dir") + "/target/temp";
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("download.default_directory", downloadFilepath);
        chromePrefs.put("credentials_enable_service", false);
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("disable-infobars","window-size="+d.width+","+d.height);
        System.setProperty( "wdm.cachePath" , System.getProperty("user.dir") + "/target/temp" );
        //options.addArguments("--headless");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR,
                UnexpectedAlertBehaviour.ACCEPT);
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(capabilities);
        driver.manage().window().maximize();
        driver.navigate().to(setURL(url));
        Runtime.getRuntime().addShutdownHook(new Thread() {  public void run() {
            try {
                BasePage.pack("target/report/html", "target/report/html.zip");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        });

    }

    public static void openApplicationFirefox(String url) {
        activeAutomation = "firefox";
        WebDriverManager.firefoxdriver().setup();
        driver = new FirefoxDriver();
        driver.manage().window().maximize();
        driver.navigate().to(setURL(url));
    }

    public static void openApplicationEdge(String url) {
        activeAutomation = "edge";
        WebDriverManager.edgedriver().setup();
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        driver.navigate().to(setURL(url));
    }

    public static void openApplicationIE(String url) {
        activeAutomation = "ie";
        System.setProperty("webdriver.ie.driver", "src/test/resources/drivers/IEDriverServer.exe");
        DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
        capabilities.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
        driver = new InternetExplorerDriver(capabilities);
        driver.manage().window().maximize();
        driver.navigate().to(setURL(url));
    }

    public static void openApplicationSikuli(String applicationPath) {
        startSikuli();
        sikuliApp = App.open(applicationPath);
        System.out.println("Command: " + applicationPath);
    }

    public static void startSikuli() {
        activeAutomation = "sikuli";
        sikuli = new Screen();
        for (String directory : Utils.getSubDirectoriesNames("src/test/resources/sikuli-images/")) {
            ImagePath.add("src/test/resources/sikuli-images/" + directory);
        }
        Settings.MoveMouseDelay = 0.07F;
        Settings.ObserveScanRate = 60F;
        Settings.ObserveMinChangedPixels = 1;
    }

    public static void openApplicationChromeMobile(String url) {
        activeAutomation = "chromemobile";
        String downloadFilepath = System.getProperty("user.dir") + "/target/temp";
        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("download.default_directory", downloadFilepath);
        chromePrefs.put("credentials_enable_service", false);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);
        options.addArguments("disable-infobars");
        options.addArguments("--disable-print-preview");

        if (System.getProperty("os.name").contains("Windows"))
            System.setProperty("webdriver.chrome.driver", "src/test/resources/drivers/chromedriver.exe");

        Map<String, String> mobileEmulation = new HashMap<String, String>();
        mobileEmulation.put("deviceName", "iPhone X");
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        options.setExperimentalOption("mobileEmulation", mobileEmulation);

        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        try {
            driver = new RemoteWebDriver(new URL("http://" + Utils.getTestProperty("selenium.host") + ":4444/wd/hub"), capabilities);
        } catch (Exception e) {
            driver = new ChromeDriver(capabilities);
        }
        driver.manage().window().maximize();
        driver.navigate().to(url);
    }

    public static String setURL(String url) {
        return Utils.getProperty(Utils.loadProperties(System.getProperty("user.dir") + "\\src\\test\\resources\\testsettings.properties"), url);
    }

}