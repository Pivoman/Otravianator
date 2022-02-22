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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {
    static int mainVillageX;
    static int mainVillageY;
    static int maxDistance = 1;
    static List<Oasis> oasisList = new ArrayList<>();
    static List<Oasis> optimalOasisList = new ArrayList<>();
    static List<Oasis> hardOasisList = new ArrayList<>();
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
        System.out.println("Starting Otravianator...");
        login();
        scanWholeMap();
        while(true) {
            if (isHeroHome()) {
                if (!optimalOasisList.isEmpty()) {
//                    Oasis oasis = optimalOasisList.remove(0);
                    Iterator<Oasis> iterator = optimalOasisList.iterator();
                    Oasis oasis = iterator.next();
                    checkHeroHPAndHeal();
                    goToMap();
                    setCoordinates(oasis.x, oasis.y);
                    resolveCell(oasis.x, oasis.y);
                    iterator.remove();
                } else {
//                    scanOnlyOasis();
//                    if (!optimalOasisList.isEmpty()) {
                        maxDistance++;
                        System.out.println("maxDistance increased to " + maxDistance);
                        oasisList.clear();
                        scanWholeMap();
//                    }
                }
            } else {
                System.out.println("Sleeping some seconds.");
                Thread.sleep(38984);
                driver.navigate().refresh();
            }
        }
    }

    private static void scanWholeMap() throws InterruptedException {
        System.out.println("Scanning whole map.");
        int mainVillageX = Integer.parseInt(prop.getProperty("mainVillageX"));
        int mainVillageY = Integer.parseInt(prop.getProperty("mainVillageY"));
        goToMap();
        for (int x = mainVillageX-maxDistance; x <= mainVillageX+maxDistance; x++) {
            for (int y = mainVillageY-maxDistance; y <= mainVillageY+maxDistance; y++) {
                setCoordinates(x, y);
                resolveCell(x, y);
            }
        }
    }

    private static void scanOnlyOasis() throws InterruptedException {
        System.out.println("Scanning oasis only.");
        goToMap();
        for (Oasis oasis : oasisList) {
            setCoordinates(oasis.x, oasis.y);
            resolveCell(oasis.x, oasis.y);
        }
    }

    private static void resolveCell(int x, int y) throws InterruptedException {
        System.out.println("Resolving cell [" + x + " | " + y + "]");
        waitForElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]"), 5000);
        driver.findElement(By.xpath("//div[contains(@style, 'overflow: hidden; position: absolute;')]")).click();
        waitForElement(By.xpath("//div[contains(@class, 'dialogCancelButton')]"), 5000);
        if (isDisplayed(By.xpath("//h1[contains(., 'voln')]"))) {
            saveOasis(x, y);
            if (!isDisplayed(By.xpath("//table[@id='troop_info']//td[.='žádné']"))) {
                if (isHeroHome() && isOptimalStrength()) {
                    attackOasis(x,y);
                    goToMap();
                    return;
                } else {
                    if (isOptimalStrength()) {
                        saveOptimalOasis(x, y);
                    } else {
                        saveHardOasis(x, y);
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

    private static void setCoordinates(int x, int y) {
        System.out.println("Setting coordinates [" + x + " | " + y + "]");
        driver.findElement(By.xpath("//input[@id='xCoordInputMap']")).clear();
        driver.findElement(By.xpath("//input[@id='xCoordInputMap']")).sendKeys(Integer.toString(x));
        driver.findElement(By.xpath("//input[@id='yCoordInputMap']")).clear();
        driver.findElement(By.xpath("//input[@id='yCoordInputMap']")).sendKeys(Integer.toString(y));
        driver.findElement(By.xpath("//button[@value='OK']")).click();
    }

    private static void goToMap() throws InterruptedException {
        System.out.println("Going to map.");
        waitForElement(By.xpath("//a[@class='map']"), 5000);
        driver.findElement(By.xpath("//a[@class='map']")).click();
        waitForElement(By.xpath("//input[@id='xCoordInputMap']"), 5000);
    }

    private static void closeModal() {
        System.out.println("Closing modal: " + driver.findElement(By.xpath("//h1[@class='titleInHeader']")).getText());
        driver.findElement(By.xpath("//div[contains(@class, 'dialogCancelButton')]")).click();
    }

    private static void saveOasis(int x, int y) {
        oasisList.add(new Oasis(x, y));
        System.out.println("[oasisList]: " + oasisList.toString());
    }

    private static void saveOptimalOasis(int x, int y) {
        optimalOasisList.add(new Oasis(x, y));
        System.out.println("[optimalOasisList]: " + optimalOasisList.toString());
    }

    private static void saveHardOasis(int x, int y) {
        hardOasisList.add(new Oasis(x, y));
        System.out.println("[hardOasisList]: " + hardOasisList.toString());
    }

    private static boolean isHeroHome() {
        boolean isHeroHome = !exists(By.xpath("//*[@class='heroRunning']"));
        System.out.println("Hero home: " + isHeroHome);
        return isHeroHome;
    }

    private static void checkHeroHPAndHeal() throws InterruptedException {
        System.out.println("Checking hero's HP.");
        driver.findElement(By.id("heroImageButton")).click();
        String origHp = driver.findElement(By.xpath("//tr[@class='attribute health tooltip']/td[2]/span")).getText();
        int hp = Integer.parseInt(origHp.replace("‭‭", "").replace("\u202C%\u202C", ""));
        if (hp < 40) {
            System.out.println("Hero under 40%, getting exps.");
            List<WebElement> villageList = driver.findElements(By.xpath("//*[contains(@class, 'listEntry')]"));
            for (WebElement village: villageList) {
                village.click();
                driver.findElement(By.id("questmasterButton")).click();
                List<WebElement> xpList = driver.findElements(By.xpath("//button/div[.='Vyzvednout']"));
                for (WebElement xp: xpList) {
                    xp.click();
                    Thread.sleep(2000);
                }
            }
        }
        if (hp < 40) {
            System.out.println("Hero still under 40%, healing 20.");
            driver.findElement(By.id("item_120031")).click();
            driver.findElement(By.xpath("//div[@id='dialogContent']/input[@id='amount']")).sendKeys("20");
            driver.findElement(By.xpath("//button[class='green dialogButtonOk ok textButtonV1']")).click();
        }
        if (hp < 40) {
            System.out.println("Low health. Exiting...");
            System.exit(0);
        }
    }

    private static void attackOasis(int x, int y) throws InterruptedException {
        if (isHeroHome()){
            System.out.println("Attacking oasis [" + x + "|" + y + "]");
            //get and find
            driver.findElement(By.xpath("//a[.='Prozkoumat volnou oázu']")).click();
            if (!isDisplayed(By.xpath("//img[@alt='Theutates Blesk']/../span"))) {
                driver.findElement(By.xpath("//img[@alt='Theutates Blesk']/../a")).click();
                System.out.println("Adding " + driver.findElement(By.xpath("//img[@alt='Theutates Blesk']/../a")).getText() + "x Blesks");
            }
            if (isDisplayed(By.xpath("//img[@alt='Hrdina']/../a"))) {
                driver.findElement(By.xpath("//img[@alt='Hrdina']/../a")).click();
            }
            driver.findElement(By.xpath("//label[contains(., 'Útok: loupež')]")).click();
            driver.findElement(By.xpath("//button[contains(., 'Odeslat')]")).click();
//        saveArrivalTime();
            waitForElement(By.xpath("//button[contains(., 'Potvrdit')]"), 5000);
            driver.findElement(By.xpath("//button[contains(., 'Potvrdit')]")).click();
            System.err.println("CHAAAARGE!");
            System.out.println("List of optimal oases is now: " + optimalOasisList.toString());
        } else {
            System.err.println("Tried to attack, but hero is not home!");
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
        System.out.println("Arrival time set to " + arrivalTime);
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
        System.out.println("Oasis total strength is " + totalStrength);
        return totalStrength < Integer.parseInt(prop.getProperty("maxOasisStrength"));
    }

    private static void login() {
        driver.get("https://ts2.x1.asia.travian.com/");
        driver.findElement(By.xpath("//input[@name='name']")).clear();
        driver.findElement(By.xpath("//input[@name='name']")).sendKeys(prop.getProperty("username"));
        driver.findElement(By.xpath("//input[@name='password']")).clear();
        driver.findElement(By.xpath("//input[@name='password']")).sendKeys(prop.getProperty("password"));
        driver.findElement(By.xpath("//button[@value='Login']")).click();
        driver.findElement(By.xpath("//span[.='" + prop.getProperty("mainVillage") + "']")).click();
    }

    public static boolean isDisplayed(By by) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            try {
                driver.findElement(by).isDisplayed();
                return true;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
        return false;
    }

    public static boolean exists(By by) {
        return !driver.findElements(by).isEmpty();
    }

    public static void waitForElement(WebElement element, int sleepTime) throws InterruptedException{
        WebDriverWait wait = new WebDriverWait(driver, sleepTime);
        wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public static void waitForElement(By by, int sleepTime) throws InterruptedException{
        WebDriverWait wait = new WebDriverWait(driver, sleepTime);
        wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    private static WebDriver getWebDriver() {
        WebDriverManager.chromedriver().browserVersion("77.0.3865.40").setup();
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
}
