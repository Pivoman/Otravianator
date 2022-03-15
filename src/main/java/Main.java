import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class Main {
    static String mainVillage;
    static int mainVillageX;
    static int mainVillageY;
    static int maxDistance = 2;
    static List<Oasis> oasisList = new ArrayList<>();
    static List<Oasis> optimalOasisList = new ArrayList<>();
    static List<Oasis> badOasisList = new ArrayList<>();
    static WebDriver driver = getWebDriver();
    static Properties prop;

    static {
        try {
            prop = getConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int arrivalTime = 0;

    public static void main(String[] args) throws InterruptedException {
        log("Starting Otravianator...");
        login();
        checkHeroHPAndHeal();
        getActualVillage();
        scanWholeMap();
        while (true) {
            try {
                if (isHeroHome()) {
                    if (!optimalOasisList.isEmpty()) {
                        Iterator<Oasis> iterator = optimalOasisList.iterator();
                        Oasis oasis = iterator.next();
                        checkHeroHPAndHeal();
                        goToMap();
                        setCoordinates(oasis.x, oasis.y);
                        resolveCell(oasis.x, oasis.y);
                        iterator.remove();
                        logE("[optimalOasisList after remove]: " + optimalOasisList.toString());
                    } else {
                        maxDistance = maxDistance >= 7 ? 1 : maxDistance + 1;
                        logE("maxDistance changed to " + maxDistance);
                        oasisList.clear();
                        badOasisList.clear();
                        scanWholeMap();
                        logE("[optimalOasisList after scan]: " + optimalOasisList.toString());
                    }
                } else {
                    log("Sleeping some seconds.");
                    Thread.sleep(maxDistance * 10846L);
                    driver.navigate().refresh();
                }
            } catch (Exception e) {
                if (isDisplayed(By.xpath("//div[.='Maintenance']"))) {
                    logE("Maintenance - Sleeping some seconds.");
                    Thread.sleep(10846);
                    driver.navigate().refresh();
                } else {
                    throw e;
                }
            }
        }
    }

    private static void scanWholeMap() throws InterruptedException {
        log("Scanning whole map.");
        goToMap();
        for (int x = mainVillageX - maxDistance; x <= mainVillageX + maxDistance; x++) {
            for (int y = mainVillageY - maxDistance; y <= mainVillageY + maxDistance; y++) {
                setCoordinates(x, y);
                resolveCell(x, y);
            }
        }
    }

    private static void scanOnlyOasis() throws InterruptedException {
        log("Scanning oasis only.");
        goToMap();
        for (Oasis oasis : oasisList) {
            setCoordinates(oasis.x, oasis.y);
            resolveCell(oasis.x, oasis.y);
        }
    }

    private static void getActualVillage() throws InterruptedException {
        goToHeroSummary();
        mainVillage = driver.findElement(By.xpath("//*[contains(@class, 'heroStatusMessage')]/span/a")).getText();
        String rawX = driver.findElement(By.xpath("//div[contains(@class, 'listEntry')]//span[.='" + mainVillage + "']/../../..//span[@class='coordinateX']")).getText();
        String rawY = driver.findElement(By.xpath("//div[contains(@class, 'listEntry')]//span[.='" + mainVillage + "']/../../..//span[@class='coordinateY']")).getText();
        mainVillageX = parseCoordinateInteger(rawX);
        mainVillageY = parseCoordinateInteger(rawY);
    }

    private static int parseCoordinateInteger(String raw) {
        String result = raw
                .replaceAll("\u202D", "")
                .replaceAll("\u202C", "")
                .replace("(", "")
                .replace(")", "")
                .replace("−", "-");
        return Integer.parseInt(result);
    }

    private static void goToMap() throws InterruptedException {
        log("Going to map.");
        waitForElement(By.xpath("//a[@class='map']"), 5);
        driver.findElement(By.xpath("//a[@class='map']")).click();
        waitForElement(By.xpath("//input[@id='xCoordInputMap']"), 5);
    }

    private static void goToHeroSummary() throws InterruptedException {
        log("Going to hero summary.");
        waitForElement(By.id("heroImageButton"), 5);
        driver.findElement(By.id("heroImageButton")).click();
        waitForElement(By.xpath("//*[@class='heroStatusMessage ']"), 5);
    }

    private static void setCoordinates(int x, int y) throws InterruptedException {
        log("Setting coordinates [" + x + " | " + y + "]");
        driver.findElement(By.xpath("//input[@id='xCoordInputMap']")).clear();
        driver.findElement(By.xpath("//input[@id='xCoordInputMap']")).sendKeys(Integer.toString(x));
        driver.findElement(By.xpath("//input[@id='yCoordInputMap']")).clear();
        driver.findElement(By.xpath("//input[@id='yCoordInputMap']")).sendKeys(Integer.toString(y));
        driver.findElement(By.xpath("//button[@value='OK']")).click();
        Thread.sleep(500);
    }

    private static void resolveCell(int x, int y) throws InterruptedException {
        log("Resolving cell [" + x + " | " + y + "]");
        waitForElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]"), 5);
        driver.findElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]")).click();
//        if (!isDisplayed(By.xpath("//div[contains(@class, 'dialogCancelButton')]"))) {
//            waitForElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]"), 5000);
//            driver.findElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]")).click();
//        }
        waitForElement(By.xpath("//a[.='Vycentrovat mapu']"), 30000);
        String title = driver.findElement(By.xpath("//h1[@class='titleInHeader']")).getText();
        if (title.contains("voln")) {
            saveOasis(x, y);
            if (!isDisplayed(By.xpath("//table[@id='troop_info']//td[.='žádné']"))) {
                if (isHeroHome() && isOptimalStrength()) {
                    attackOasis(x, y);
                    goToMap();
                    return;
                } else {
                    if (isOptimalStrength()) {
                        saveOptimalOasis(x, y);
                    } else {
                        saveBadOasis(x, y);
                    }

                    closeModal();
                }
            } else {
                closeModal();
            }
        } else {
            closeModal();
        }
    }

    private static void closeModal() {
        log("Closing modal: " + driver.findElement(By.xpath("//h1[@class='titleInHeader']")).getText());
        driver.findElement(By.xpath("//div[contains(@class, 'dialogCancelButton')]")).click();
    }

    private static void saveOasis(int x, int y) {
        oasisList.add(new Oasis(x, y));
        log("[oasisList]: " + oasisList.toString());
    }

    private static void saveOptimalOasis(int x, int y) {
        optimalOasisList.add(new Oasis(x, y));
        logE("[optimalOasisList]: " + optimalOasisList.toString());
    }

    private static void saveBadOasis(int x, int y) {
        badOasisList.add(new Oasis(x, y));
        log("[badOasisList]: " + badOasisList.toString());
    }

    private static boolean isHeroHome() {
        boolean isHeroHome = !exists(By.xpath("//*[@class='heroRunning']"));
        log("Hero home: " + isHeroHome);
        return isHeroHome;
    }

    private static void checkHeroHPAndHeal() throws InterruptedException {
        log("Checking hero's HP.");
        goToHeroSummary();
        String origHp = driver.findElement(By.xpath("//tr[@class='attribute health tooltip']/td[2]/span")).getText();
        int hp = Integer.parseInt(origHp.replace("‭‭", "").replace("\u202C%\u202C", ""));
        if (hp < 40) {
            log("Hero have " + hp + "%, getting exps.");
            waitForElement(By.xpath("//*[contains(@class, 'listEntry')]"), 5);
            List<WebElement> villageList = driver.findElements(By.xpath("//*[contains(@class, 'listEntry')]/a"));
            for (int i = 0; i < villageList.size(); i++) {
                Thread.sleep(2000);
                driver.findElement(By.xpath("//*[contains(@class, 'villageList')]/*[" + (i + 1) + "]")).click();
                driver.findElement(By.id("questmasterButton")).click();
                Thread.sleep(2000);
                List<WebElement> xpList = driver.findElements(By.xpath("//button/div[.='Vyzvednout']"));
                for (int j = 0; j < xpList.size(); j++) {
                    driver.findElement(By.xpath("//button/div[.='Vyzvednout'][" + (i + 1) + "]")).click();
                    Thread.sleep(2000);
                }
            }
        }
        if (hp < 40) {
            log("Hero have " + hp + "%, healing.");
            goToHeroSummary();
            driver.findElement(By.id("item_120031")).click();
            waitForElement(By.xpath("//div[@id='dialogContent']/input[@id='amount']"), 5);
            driver.findElement(By.xpath("//div[@id='dialogContent']/input[@id='amount']")).clear();
            driver.findElement(By.xpath("//div[@id='dialogContent']/input[@id='amount']")).sendKeys("20");
            driver.findElement(By.xpath("//button[.='OK']")).click();
            Thread.sleep(5000);
        }
        if (hp < 40) {
            log("Low health. Exiting...");
            System.exit(0);
        }
    }

    private static void attackOasis(int x, int y) throws InterruptedException {
        if (isHeroHome()) {
            log("Attacking oasis [" + x + "|" + y + "]");
            //get and find
            driver.findElement(By.xpath("//a[.='Prozkoumat volnou oázu']")).click();
//            if (!isDisplayed(By.xpath("//img[@alt='Theutates Blesk']/../span"))) {
//                driver.findElement(By.xpath("//img[@alt='Theutates Blesk']/../a")).click();
//                log("Adding " + driver.findElement(By.xpath("//img[@alt='Theutates Blesk']/../a")).getText() + "x Blesks");
//            }
            if (!isDisplayed(By.xpath("//img[@alt='Hrdina']/../a"))) {
                driver.findElement(By.xpath("//div[contains(@class, 'listEntry')]//span[.='" + mainVillage + "']")).click();
            }
            driver.findElement(By.xpath("//img[@alt='Hrdina']/../a")).click();
            driver.findElement(By.xpath("//label[contains(., 'Útok: loupež')]")).click();
            driver.findElement(By.xpath("//button[contains(., 'Odeslat')]")).click();
//        saveArrivalTime();
            waitForElement(By.xpath("//button[contains(., 'Potvrdit')]"), 5);
            driver.findElement(By.xpath("//button[contains(., 'Potvrdit')]")).click();
            driver.findElement(By.xpath("//span[contains(@class,'coordinateX')][contains(.,'" + Math.abs(x) + "')]/../span[contains(@class,'coordinateY')][contains(.,'" + Math.abs(y) + "')]")).click();
            driver.findElement(By.xpath("//a[.='Prozkoumat volnou oázu']")).click();
            driver.findElement(By.xpath("//input[@name='troops[0][t4]']")).sendKeys("20");
            driver.findElement(By.xpath("//label[contains(., 'Útok: loupež')]")).click();
            driver.findElement(By.xpath("//button[contains(., 'Odeslat')]")).click();
            waitForElement(By.xpath("//button[contains(., 'Potvrdit')]"), 5);
            driver.findElement(By.xpath("//button[contains(., 'Potvrdit')]")).click();
            logE("CHAAAARGE!");
            logE("List of optimal oases is now: " + optimalOasisList.toString());
        } else {
            logE("Tried to attack, but hero is not home!");
        }
    }

    private static void saveArrivalTime() {
        String arrivalText = driver.findElement(By.xpath("//div[@class='in']")).getText();
        arrivalText = arrivalText.replace("za ", "").replace(" hod.", "");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(arrivalText);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        assert date != null;
        arrivalTime = (int) date.getTime();
        log("Arrival time set to " + arrivalTime);
    }

    private static boolean isOptimalStrength() {
        int totalStrength = 10;
        List<WebElement> troopInfoList = driver.findElements(By.xpath("//table[@id='troop_info']//td[@class='val']"));
        for (WebElement troopInfo : troopInfoList) {
            int count = Integer.parseInt(troopInfo.getText());
            String animalStr = troopInfo.findElement(By.xpath("../td/img")).getAttribute("alt");
            Animals animal = Animals.valueOfLabel(animalStr);
            assert animal != null;
            totalStrength += count * animal.strength;
        }
        log("Oasis total strength is " + totalStrength);
        return 100 < totalStrength && totalStrength < Integer.parseInt(prop.getProperty("maxOasisStrength"));
    }

    private static void login() {
        driver.get("https://ts2.x1.asia.travian.com/");
        driver.findElement(By.xpath("//input[@name='name']")).clear();
        driver.findElement(By.xpath("//input[@name='name']")).sendKeys(prop.getProperty("username"));
        driver.findElement(By.xpath("//input[@name='password']")).clear();
        driver.findElement(By.xpath("//input[@name='password']")).sendKeys(prop.getProperty("password"));
        driver.findElement(By.xpath("//button[@value='Login']")).click();
        driver.findElement(By.xpath("//span[.='" + mainVillage + "']")).click();
    }

    public static boolean isDisplayed(By by) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                driver.findElement(by).isDisplayed();
                return true;
            } catch (Exception e) {
                Thread.sleep(30);
            }
        }
        return false;
    }

    public static boolean exists(By by) {
        return !driver.findElements(by).isEmpty();
    }

    public static void waitForElement(WebElement element, int sleepTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sleepTime));
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public static void waitForElement(By by, int sleepTime) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(sleepTime));
        wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    private static WebDriver getWebDriver() {
        WebDriverManager.chromedriver().browserVersion("98.0.4758.102").setup();
        WebDriverManager.chromedriver().driverVersion("98.0.4758.102").setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("enable-automation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("--disable-gpu");
        return new ChromeDriver(options);
    }

    private static Properties getConfig() throws IOException {
        Properties prop = new Properties();
        InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties");
        prop.load(input);
        return prop;
    }

    private static void log(String text) {
        System.out.println("[" + new Timestamp(System.currentTimeMillis()) + "]: " + text);
    }

    private static void logE(String text) {
        System.err.println("[" + new Timestamp(System.currentTimeMillis()) + "]: " + text);
    }
}
