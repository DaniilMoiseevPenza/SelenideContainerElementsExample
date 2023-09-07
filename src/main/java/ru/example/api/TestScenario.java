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

import com.codeborne.selenide.Selenide;
import lombok.extern.slf4j.Slf4j;
import ru.example.ScopedVariables;

import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Главный класс, отвечающий за сопровождение тестовых шагов
 */
@Slf4j
public final class TestScenario {

    private static final TestScenario INSTANCE = new TestScenario();

    /**
     * Среда прогона тестов, хранит в себе: Cucumber.Scenario,
     * переменные, объявленные пользователем в сценарии и страницы, тестирование которых будет производиться
     */
    private static Environment environment;

    private TestScenario() {
    }

    public static TestScenario getInstance() {
        return INSTANCE;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public static void setEnvironment(Environment environment) {
        TestScenario.environment = environment;
    }

    public static void sleep(int seconds) {
        Selenide.sleep(MILLISECONDS.convert(seconds, SECONDS));
    }

    /**
     * Получение страницы, тестирование которой производится в данный момент
     */
    public Page getCurrentPage() {
        return environment.getPages().getCurrentPage();
    }

    /**
     * Задание страницы, тестирование которой производится в данный момент
     */
    public void setCurrentPage(Page page) {
        if (page == null) {
            throw new IllegalArgumentException("Происходит переход на несуществующую страницу. " +
                    "Проверь аннотации @Name у используемых страниц");
        }
        environment.getPages().setCurrentPage(page);
    }

    /**
     * Позволяет получить доступ к полям и методам конкретной страницы, которая передается в метод в качестве аргумента.
     * Пример использования: {@code withPage(Page.class, page -> { some actions with Page methods});}
     * Проверка отображения всех элементов страницы выполняется всегда
     *
     * @param clazz класс страницы, доступ к полям и методам которой необходимо получить
     */
    public static <T extends Page> void withPage(Class<T> clazz, Consumer<T> consumer) {
        Pages.withPage(clazz, true, consumer);
    }

    /**
     * Позволяет получить доступ к полям и методам конкретной страницы.
     * Пример использования: {@code withPage(Page.class, page -> { some actions with Page methods});}
     * Проверка отображения всех элементов страницы опциональна
     *
     * @param clazz                   класс страницы, доступ к полям и методам которой необходимо получить
     * @param checkIfElementsAppeared флаг, отвечающий за проверку отображения всех элементов страницы, не помеченных аннотацией @Optional
     */
    public static <T extends Page> void withPage(Class<T> clazz, boolean checkIfElementsAppeared, Consumer<T> consumer) {
        Pages.withPage(clazz, checkIfElementsAppeared, consumer);
    }

    /**
     * Возвращает текущий сценарий (Cucumber.api)
     */
    public io.cucumber.java.Scenario getScenario() {
        return this.getEnvironment().getScenario();
    }

    /**
     * Получение списка страниц
     */
    public Pages getPages() {
        return this.getEnvironment().getPages();
    }

    public Page getPage(String name) {
        return this.getEnvironment().getPage(name);
    }

    /**
     * Выводит дополнительный информационный текст в отчет (уровень логирования INFO)
     */
    public void write(Object object) {
        this.getEnvironment().write(object);
    }

    /**
     * Получение переменной по имени, заданного пользователем, из пула переменных "variables" в Environment
     *
     * @param name - имя переменной, для которой необходимо получить ранее сохраненное значение
     */
    public Object getVar(String name) {
        Object obj = this.getEnvironment().getVar(name);
        if (obj == null) {
            throw new IllegalArgumentException("Переменная " + name + " не найдена");
        }
        return obj;
    }

    /**
     * Получение переменной без проверки на NULL
     */
    public Object tryGetVar(String name) {
        return this.getEnvironment().getVar(name);
    }

    /**
     * Получение страницы по классу с возможностью выполнить проверку отображения элементов страницы
     *
     * @param clazz                   - класс страницы, которую необходимо получить
     * @param checkIfElementsAppeared - флаг, определяющий проверку отображения элементов на странице
     */
    public <T extends Page> Page getPage(Class<T> clazz, boolean checkIfElementsAppeared) {
        return Pages.getPage(clazz, checkIfElementsAppeared).initialize();
    }

    /**
     * Получение страницы по классу (проверка отображения элементов страницы не выполняется)
     *
     * @param clazz - класс страницы, которую необходимо получить
     */
    public <T extends Page> T getPage(Class<T> clazz) {
        return Pages.getPage(clazz, true);
    }

    /**
     * Получение страницы по классу и имени (оба параметра должны совпадать)
     *
     * @param clazz - класс страницы, которую необходимо получить
     * @param name  - название страницы, заданное в аннотации @Name
     */
    public <T extends Page> T getPage(Class<T> clazz, String name) {
        return this.getEnvironment().getPage(clazz, name);
    }

    /**
     * Заменяет в строке все ключи переменных из пула переменных "variables" в классе Environment на их значения
     *
     * @param stringToReplaceIn строка, в которой необходимо выполнить замену (не модифицируется)
     */
    public String replaceVariables(String stringToReplaceIn) {
        return this.getEnvironment().replaceVariables(stringToReplaceIn);
    }

    /**
     * Добавление переменной в пул "variables" в классе Environment
     *
     * @param name   Имя переменной заданное пользователем, для которого сохраняется значение. Является ключом в пуле variables в классе Environment
     * @param object значение, которое нужно сохранить в переменную
     */
    public void setVar(String name, Object object) {
        this.getEnvironment().setVar(name, object);
    }

    /**
     * Получение всех переменных из пула "variables" в классе Environment
     */
    public ScopedVariables getVars() {
        return this.getEnvironment().getVars();
    }
}
