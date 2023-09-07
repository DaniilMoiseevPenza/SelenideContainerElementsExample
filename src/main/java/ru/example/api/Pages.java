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
import com.google.common.collect.Maps;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Предназначен для хранения страниц, используемых при прогоне тестов
 */
public final class Pages {

    /**
     * Страницы, на которых будет производиться тестирование < Имя, Страница >
     */
    private final Map<String, Page> pagesMap;

    /**
     * Страница, на которой в текущий момент производится тестирование
     */
    private Page currentPage;

    public Pages() {
        pagesMap = Maps.newHashMap();
    }


    /**
     * Возвращает текущую страницу, на которой в текущий момент производится тестирование
     */
    public Page getCurrentPage() {
        if (currentPage == null) throw new IllegalStateException("Текущая страница не задана");
        return currentPage.initialize();
    }

    /**
     * Задает текущую страницу по ее имени
     */
    public void setCurrentPage(Page page) {
        this.currentPage = page;
    }

    /**
     * Реализация анонимных методов со страницей в качестве аргумента
     *
     * @param clazz                   класс страницы
     * @param checkIfElementsAppeared проверка всех не помеченных "@Optional" элементов
     */
    public static <T extends Page> void withPage(Class<T> clazz, boolean checkIfElementsAppeared, Consumer<T> consumer) {
        T page = getPage(clazz, checkIfElementsAppeared);
        consumer.accept(page);
    }

    /**
     * Получение страницы из "pages" по имени
     */
    public Page get(String pageName) {
        return Selenide.page(getPageFromPagesByName(pageName)).initialize();
    }

    /**
     * Получение страницы по классу
     */
    @SuppressWarnings("unchecked")
    public <T extends Page> T get(Class<T> clazz, String name) {
        Page page = Selenide.page(getPageFromPagesByName(name)).initialize();

        if (!clazz.isInstance(page)) {
            throw new IllegalStateException(name + " page is not a instance of " + clazz + ". Named page is a " + page);
        }
        return (T) page;
    }

    private Map<String, Page> getPageMapInstanceInternal() {
        return pagesMap;
    }

    private Page getPageFromPagesByName(String pageName) throws IllegalArgumentException {
        Page page = getPageMapInstanceInternal().get(pageName);
        if (page == null)
            throw new IllegalArgumentException(pageName + " page is not declared in a list of available pages");
        return page;
    }

    /**
     * Добавление инстантизированной страницы в "pages" с проверкой на NULL
     */
    public <T extends Page> void put(String pageName, T page) throws IllegalArgumentException {
        if (page == null)
            throw new IllegalArgumentException("Была передана пустая страница");
        pagesMap.put(pageName, page);
    }

    /**
     * Получение страницы по классу с возможностью выполнить проверку элементов страницы
     */
    public static <T extends Page> T getPage(Class<T> clazz, boolean checkIfElementsAppeared) {
        T page = Selenide.page(clazz);
        if (checkIfElementsAppeared) {
            page.initialize().isAppeared();
        }
        return page;
    }

    /**
     * Добавление страницы в "pages" по классу
     */
    @SneakyThrows
    public void put(String pageName, Class<? extends Page> page) {
        if (page == null)
            throw new IllegalArgumentException("Была передана пустая страница");
        Constructor<? extends Page> constructor = page.getDeclaredConstructor();
        Page p = constructor.newInstance();
        pagesMap.put(pageName, p);
    }
}
