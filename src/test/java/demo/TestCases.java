package demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dev.failsafe.internal.util.Durations;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.time.Duration;


public class TestCases {
    ChromeDriver driver;

    @BeforeTest
    public void startBrowser() {
        System.setProperty("java.util.logging.config.file", "logging.properties");

        // Initialize WebDriverManager if uncommented in future
        // WebDriverManager.chromedriver().timeout(30).setup();

        ChromeOptions options = new ChromeOptions();
        LoggingPreferences logs = new LoggingPreferences();

        logs.enable(LogType.BROWSER, Level.ALL);
        logs.enable(LogType.DRIVER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logs);
        options.addArguments("--remote-allow-origins=*");

        System.setProperty(ChromeDriverService.CHROME_DRIVER_LOG_PROPERTY, "build/chromedriver.log");

        driver = new ChromeDriver(options);
        driver.manage().window().maximize();
    }

    @Test(priority = 1)
    public void testCase01() {
        System.out.println("START : testCase01 Started --> ");
        ArrayList<Map<String, Object>> hockeyTeams = new ArrayList<>();

        try {
            driver.get("https://www.scrapethissite.com/pages/"); // Replace with the actual URL

            // Click on "Hockey Teams: Forms, Searching and Pagination"
            driver.findElement(By.linkText("Hockey Teams: Forms, Searching and Pagination")).click();
            System.out.println("Pass : click on 'Hockey Teams' successfull");
            

            // Iterate through 4 pages
            for (int i = 0; i < 4; i++) {
                List<WebElement> rows = driver
                        .findElements(By.xpath("//*[@class='pct text-success' or @class='pct text-danger']"));
                for (WebElement row : rows) {
                    WebElement nameElement = row.findElement(By.xpath("./preceding-sibling::td[@class='name']"));
                    String teamName = nameElement.getText();
                    WebElement yearElement = row.findElement(By.xpath("./preceding-sibling::td[@class='year']"));
                    String year = yearElement.getText();
                    double winPercentage = Double.parseDouble(row.getText().replace("%", ""));

                    if (winPercentage < 0.40) {
                        Map<String, Object> teamData = new HashMap<>();
                        teamData.put("Epoch Time of Scrape", Instant.now().getEpochSecond());
                        teamData.put("Team Name", teamName);
                        teamData.put("Year", year);
                        teamData.put("Win %", winPercentage);
                        hockeyTeams.add(teamData);
                    }
                }

                // Click next page if not the last iteration
                if (i < 3) {
                    WebElement nextPageButton = driver.findElement(By.xpath("//*[@aria-label='Next']"));
                    if (nextPageButton.isDisplayed()) {
                        nextPageButton.click();
                    }
                }
            }

            // Convert ArrayList to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(new File("hockey-team-data.json"), hockeyTeams);

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("END : testCase01 Ended...");
    }

    @Test(priority = 2)
    public void testCase02() {
        System.out.println("START : testCase02 Started --> ");
        ArrayList<Map<String, Object>> oscarWinners = new ArrayList<>();
        String outputFilePath = "output/oscar-winner-data.json"; // Ensure 'output' directory exists

        try {
            driver.get("https://www.scrapethissite.com/pages/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.partialLinkText("Oscar Winning Films")));

            // Click on "Oscar Winning Films"
            driver.findElement(By.partialLinkText("Oscar Winning Films")).click();

            // Get all years
            List<WebElement> years = driver.findElements(By.xpath("//*[@class='year-link']"));
            for (WebElement yearLink : years) {
                String year = yearLink.getText();
                yearLink.click();

                // Get top 5 movies for the year
                List<WebElement> rows = driver.findElements(By.xpath("//*[@class='film-title']"));
                int count = 0;
                boolean isWinner = false;
                for (WebElement row : rows) {
                    if (count >= 5)
                        break; // Only get top 5

                    // WebElement titleElement = row.findElement(By.cssSelector("td:nth-child(1)"));
                    WebElement nominationElement = row
                            .findElement(By.xpath("./following-sibling::td[@class='film-nominations']"));
                    WebElement awardsElement = row
                            .findElement(By.xpath("./following-sibling::td[@class='film-awards']"));
                    try {
                        WebElement bestMovie = row
                                .findElement(By.xpath("./following-sibling::td[@class='film-best-picture']/i"));
                        if (!bestMovie.isDisplayed()) {
                            isWinner = true;
                        }
                    } catch (Exception e) {
                        isWinner = false;
                    }

                    String title = row.getText();
                    String nomination = nominationElement.getText();
                    String awards = awardsElement.getText();

                    Map<String, Object> movieData = new HashMap<>();
                    movieData.put("Epoch Time of Scrape", Instant.now().getEpochSecond());
                    movieData.put("Year", year);
                    movieData.put("Title", title);
                    movieData.put("Nomination", nomination);
                    movieData.put("Awards", awards);
                    movieData.put("isWinner", isWinner);

                    oscarWinners.add(movieData);
                    count++;
                }

                // Go back to the list of years
                driver.navigate().back();
            }

            // Convert ArrayList to JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            File outputFile = new File(outputFilePath);

            // Ensure the output directory exists
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            mapper.writeValue(outputFile, oscarWinners);

            // Assert that the file is present and not empty
            Assert.assertTrue(outputFile.exists(), "The JSON file does not exist.");
            Assert.assertTrue(outputFile.length() > 0, "The JSON file is empty.");

            System.out.println("JSON file created successfully at: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("END : testCase02 Ended...");
    }

    @AfterTest
    public void endTest() {
        if (driver != null) {
            driver.close();
            driver.quit();
        }
    }
}
