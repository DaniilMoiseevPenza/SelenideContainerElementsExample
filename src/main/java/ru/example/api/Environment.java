/*
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
package ru.example.api;

import io.cucumber.java.Scenario;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.example.ScopedVariables;
import ru.example.annotations.Name;

import java.util.Arrays;

/**
 * Класс, связанный с Scenario, используется для хранения страниц и переменных внутри сценария
 */
@Slf4j
public class Environment {

    /**
     * Сценарий (Cucumber.api), с которым связана среда
     */
    private Scenario scenario;
    /**
     * Переменные, объявленные пользователем внутри сценария
     * ThreadLocal обеспечивает отсутствие коллизий при многопоточном запуске
     */
    private final ThreadLocal<ScopedVariables> variables = new ThreadLocal<>();
    /**
     * Список веб-страниц, заданных пользователем, доступных для использования в сценариях
     */
    private final Pages pages = new Pages();

    public Environment(Scenario scenario) {
        this.scenario = scenario;
        initPages();
    }

    public Environment() {
        initPages();
    }

    /**
     * Метод ищет классы, аннотированные "Page.Name",
     * добавляя ссылки на эти классы в поле "pages"
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    private void initPages() {
        new AnnotationScanner().getClassesAnnotatedWith(Name.class)
                .stream()
                .map(it -> {
                    if (Page.class.isAssignableFrom(it)) {
                        return (Class<? extends Page>) it;
                    } else {
                        throw new IllegalStateException("Класс " + it.getName() + " должен наследоваться от Page");
                    }
                })
                .forEach(clazz -> pages.put(getClassAnnotationValue(clazz), clazz));
    }

    /**
     * Вспомогательный метод, получает значение аннотации "Page.Name" для класса
     *
     * @param c класс, который должен быть аннотирован "Page.Name"
     * @return значение аннотации "Page.Name" для класса
     */
    private String getClassAnnotationValue(Class<?> c) {
        return Arrays.stream(c.getAnnotationsByType(Name.class))
                .findAny()
                .map(Name::value)
                .orElseThrow(() -> new AssertionError("Не найдены аннотации Page.Name в класса " + c.getName()));
    }

    /**
     * Выводит дополнительный информационный текст в отчет (уровень логирования INFO)
     */
    public void write(Object object) {
        scenario.log(String.valueOf(object));
    }

    public ScopedVariables getVars() {
        return getVariables();
    }

    public Object getVar(String name) {
        return getVariables().get(name);
    }

    public void setVar(String name, Object object) {
        getVariables().put(name, object);
    }

    public Scenario getScenario() {
        return scenario;
    }

    public Pages getPages() {
        return pages;
    }

    public Page getPage(String name) {
        return pages.get(name);
    }

    public <T extends Page> T getPage(Class<T> clazz, String name) {
        return pages.get(clazz, name);
    }

    public String replaceVariables(String textToReplaceIn) {
        return getVariables().replaceVariables(textToReplaceIn);
    }

    private ScopedVariables getVariables() {
        if (variables.get() == null) {
            variables.set(new ScopedVariables());
        }
        return variables.get();
    }
}
