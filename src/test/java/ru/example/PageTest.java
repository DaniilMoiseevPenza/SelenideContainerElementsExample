package ru.example;/*
 * Copyright 2017 Alfa Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.cucumber.java.Scenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.example.api.Environment;
import ru.example.api.Page;
import ru.example.api.TestScenario;

import java.io.File;

import static com.codeborne.selenide.Selenide.closeWebDriver;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static ru.example.api.TestScenario.getInstance;

class PageTest {
    private static PageMock PageMock;
    private static Page page;
    static TestScenario testScenario = getInstance();

    @BeforeAll
    static void setup() {
        PageMock = new PageMock();
        Scenario scenario = mock(Scenario.class);
        testScenario.setEnvironment(new Environment(scenario));
        String inputFilePath = "src/test/resources/PageMock.html";
        String url = new File(inputFilePath).getAbsolutePath();
        testScenario.setVar("Page", "file://" + url);
        goToSelectedPageByLink("ru.example.PageMock", testScenario.getVar("Page").toString());
        page = testScenario.getEnvironment().getPage("ru.example.PageMock");
    }

    @AfterAll
    static void close() {
        closeWebDriver();
    }

    @Test
    void getBlockPositive() {
        assertThat(page.getBlock("SearchBlock"), is(notNullValue()));
    }

    public static void goToSelectedPageByLink(String pageName, String urlOrName) {
        open(urlOrName);
        sleep(1000);
        loadPage(pageName);
    }

    public static void loadPage(String nameOfPage) {
        testScenario.setCurrentPage(testScenario.getPage(nameOfPage));
        testScenario.getCurrentPage().appeared();
    }
}
