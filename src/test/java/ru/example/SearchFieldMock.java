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


import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;
import ru.example.annotations.Name;
import ru.example.annotations.Optional;
import ru.example.api.Page;

@Name("SearchBlock")
public class SearchFieldMock extends Page {

    @FindBy(name = "searchInput")
    @Name("SearchInput")
    private SelenideElement searchInput;

    @FindBy(name = "searchButton")
    @Name("SearchButton")
    private SelenideElement submitButton;

    @Optional
    @FindBy(name = "test")
    @Name("TestButton")
    private SelenideElement testButton;
}
