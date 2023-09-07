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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.ElementsContainer;
import com.codeborne.selenide.SelenideElement;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.support.FindBy;
import ru.example.annotations.Name;
import ru.example.annotations.Optional;
import ru.example.annotations.Hidden;
import ru.example.utils.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/*
 * Класс для реализации паттерна PageObject
 */
@Slf4j
public abstract class Page {
    /**
     * Стандартный таймаут ожидания элементов в миллисекундах
     */
    private static final Integer WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS = 8000;
    /**
     * Список всех элементов страницы
     */
    private Map<String, Object> namedElements = Map.of();
    /**
     * Список элементов страницы, не помеченных аннотацией "Optional" или "Hidden"
     */
    private List<SelenideElement> primaryElements = List.of();

    /**
     * Список элементов страницы, помеченных аннотацией "Hidden"
     */
    private List<SelenideElement> hiddenElements = List.of();

    /**
     * Список списочных элементов страницы, не помеченных аннотацией "Optional" или "Hidden"
     */
    private List<ElementsCollection> primaryElementCollections = List.of();
    private static final String INPUT = "input";
    private static final String NOT_DESCRIBED_ON_THE_PAGE = " не описан на странице ";

    protected Page() {
        super();
    }

    /**
     * Получение блока со страницы по имени (аннотированного "Name")
     */
    public Page getBlock(String blockName) {
        Page pageBlock = (Page) java.util.Optional.ofNullable(namedElements.get(blockName))
                .orElseThrow(() -> new IllegalArgumentException("Блок " + blockName + NOT_DESCRIBED_ON_THE_PAGE + this.getClass().getName()));
        return pageBlock.initialize();
    }

    /**
     * Получение списка блоков со страницы по имени (аннотированного "Name")
     */
    @SuppressWarnings("unchecked")
    public List<Page> getBlocksList(String listName) {
        Object value = namedElements.get(listName);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Список " + listName + NOT_DESCRIBED_ON_THE_PAGE + this.getClass().getName());
        }
        Stream<Object> s = ((List<Object>) value).stream();
        return s.map(Page::castToPage).collect(toList());
    }

    /**
     * Получение списка из элементов блока со страницы по имени (аннотированного "Name")
     */
    public List<SelenideElement> getBlockElements(String blockName) {
        return getBlock(blockName).namedElements.values().stream()
                .map(o -> ((SelenideElement) o)).collect(toList());
    }

    /**
     * Получение элемента блока со страницы по имени (аннотированного "Name")
     */
    public SelenideElement getBlockElement(String blockName, String elementName) {
        return ((SelenideElement) getBlock(blockName).namedElements.get(elementName));
    }

    /**
     * Получение элемента со страницы по имени (аннотированного "Name")
     */
    public SelenideElement getElement(String elementName) {
        return (SelenideElement) java.util.Optional.ofNullable(namedElements.get(elementName))
                .orElseThrow(() -> new IllegalArgumentException("Элемент " + elementName + NOT_DESCRIBED_ON_THE_PAGE + this.getClass().getName()));
    }

    /**
     * Получение элемента-списка со страницы по имени
     */
    public ElementsCollection getElementsList(String listName) {
        Object value = namedElements.get(listName);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Список " + listName + NOT_DESCRIBED_ON_THE_PAGE + this.getClass().getName());
        }
        FindBy listSelector = Arrays.stream(this.getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Name.class) != null && f.getDeclaredAnnotation(Name.class).value().equals(listName))
                .map(f -> f.getDeclaredAnnotation(FindBy.class))
                .findFirst().orElse(null);
        FindBy.FindByBuilder findByBuilder = new FindBy.FindByBuilder();
        return $$(findByBuilder.buildIt(listSelector, null));
    }

    /**
     * Получение текстов всех элементов, содержащихся в элементе-списке,
     * состоящего как из редактируемых полей, так и статичных элементов по имени.
     * Используется метод innerText(), который получает как видимый, так и скрытый текст из элемента,
     * обрезая перенос строк и пробелы в конце и начале строчки.
     */
    public List<String> getAnyElementsListInnerTexts(String listName) {
        List<SelenideElement> elementsList = getElementsList(listName);
        return elementsList.stream()
                .map(element -> element.getTagName().equals(INPUT)
                        ? Objects.requireNonNull(element.getValue()).trim()
                        : element.innerText().trim()
                )
                .collect(toList());
    }

    /**
     * Получение текста элемента, как редактируемого поля, так и статичного элемента по имени
     */
    public String getAnyElementText(String elementName) {
        return getAnyElementText(getElement(elementName));
    }

    /**
     * Получение текста элемента, как редактируемого поля, так и статичного элемента по значению элемента
     */
    public String getAnyElementText(SelenideElement element) {
        if (element.getTagName().equals(INPUT) || element.getTagName().equals("textarea")) {
            return element.getValue();
        } else {
            return element.getText();
        }
    }

    /**
     * Получение текстов всех элементов, содержащихся в элементе-списке,
     * состоящего как из редактируемых полей, так и статичных элементов по имени
     */
    public List<String> getAnyElementsListTexts(String listName) {
        List<SelenideElement> elementsList = getElementsList(listName);
        return elementsList.stream()
                .map(element -> element.getTagName().equals(INPUT)
                        ? element.getValue()
                        : element.getText()
                )
                .collect(toList());
    }

    /**
     * Получение всех элементов страницы, не помеченных аннотацией "Optional" или "Hidden"
     */
    public List<SelenideElement> getPrimaryElements() {
        if (primaryElements == null) {
            primaryElements = readWithWrappedElements();
        }
        return new ArrayList<>(primaryElements);
    }

    /**
     * Получение всех элементов страницы, помеченных аннотацией "Hidden"
     */
    public List<SelenideElement> getHiddenElements() {
        if (hiddenElements == null) {
            hiddenElements = readWithHiddenElements();
        }
        return new ArrayList<>(hiddenElements);
    }

    /**
     * Получения всех элементов с типом ElementsCollection, не помеченных аннотацией "Optional" или "Hidden"
     *
     * @return Возвращает все элементы с типом ElementsCollection, не помеченных аннотацией "Optional" или "Hidden"
     */
    public List<ElementsCollection> getPrimaryElementsCollections() {
        if (primaryElementCollections == null) {
            primaryElementCollections = readPrimaryElementsCollections();
        }

        return new ArrayList<>(primaryElementCollections);
    }

    /**
     * Обертка над Page.isAppeared
     * Ex: Page.appeared().doSomething();
     */
    public final Page appeared() {
        isAppeared();
        return this;
    }

    /**
     * Обертка над Page.isDisappeared
     * Ex: Page.disappeared().doSomething();
     */
    public final Page disappeared() {
        isDisappeared();
        return this;
    }

    /**
     * Проверка того, что элементы, не помеченные аннотацией "Optional", отображаются,
     * а элементы, помеченные аннотацией "Hidden", скрыты.
     */
    protected void isAppeared() {
        getPrimaryElements().forEach(elem ->
                elem.shouldBe(visible, ofMillis(WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS)));
        getHiddenElements().forEach(elem ->
                elem.shouldBe(hidden, ofMillis(WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS)));
        eachForm(Page::isAppeared);
    }

    @SuppressWarnings("unchecked")
    private void eachForm(Consumer<Page> func) {
        Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Optional.class) == null && f.getDeclaredAnnotation(Hidden.class) == null)
                .forEach(f -> {
                    if (Page.class.isAssignableFrom(f.getType())) {
                        Page page = TestScenario.getInstance().getPage((Class<? extends Page>) f.getType()).initialize();
                        func.accept(page);
                    }
                });
    }

    /**
     * Проверка, что все элементы страницы, не помеченные аннотацией "Optional" или "Hidden", исчезли
     */
    protected void isDisappeared() {
        getPrimaryElements().forEach(elem ->
                elem.shouldNotBe(exist, ofMillis(WAITING_APPEAR_TIMEOUT_IN_MILLISECONDS)));
    }

    /**
     * Обертка над Selenide.waitUntil для произвольного количества элементов
     *
     * @param condition Selenide.Condition
     * @param timeout   максимальное время ожидания для перехода элементов в заданное состояние
     * @param elements  произвольное количество selenide-элементов
     */
    public void waitElementsUntil(Condition condition, int timeout, SelenideElement... elements) {
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    /**
     * Обертка над Selenide.waitUntil для работы со списком элементов
     *
     * @param elements список selenide-элементов
     */
    public void waitElementsUntil(Condition condition, int timeout, ElementsCollection elements) {
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    /**
     * Проверка, что все переданные элементы в течении заданного периода времени
     * перешли в состояние Selenide.Condition
     *
     * @param elementNames произвольное количество строковых переменных с именами элементов
     */
    public void waitElementsUntil(Condition condition, int timeout, String... elementNames) {
        List<SelenideElement> elements = Arrays.stream(elementNames)
                .map(name -> namedElements.get(name))
                .flatMap(v -> v instanceof List ? ((List<?>) v).stream() : Stream.of(v))
                .map(Page::castToSelenideElement)
                .filter(Objects::nonNull)
                .collect(toList());
        Spectators.waitElementsUntil(condition, timeout, elements);
    }

    /**
     * Поиск элемента по имени внутри списка элементов
     */
    public static SelenideElement getButtonFromListByName(List<SelenideElement> listButtons, String nameOfButton) {
        List<String> names = new ArrayList<>();
        for (SelenideElement button : listButtons) {
            names.add(button.getText());
        }
        return listButtons.get(names.indexOf(nameOfButton));
    }

    /**
     * Приведение объекта к типу SelenideElement
     */
    private static SelenideElement castToSelenideElement(Object object) {
        if (object instanceof SelenideElement) {
            return (SelenideElement) object;
        }
        return null;
    }

    private static Page castToPage(Object object) {
        if (object instanceof Page) {
            return (Page) object;
        }
        return null;
    }


    public Page initialize() {
        namedElements = readNamedElements();
        primaryElements = readWithWrappedElements();
        hiddenElements = readWithHiddenElements();
        return this;
    }

    /**
     * Поиск и инициализации элементов страницы
     */
    private Map<String, Object> readNamedElements() {
        checkNamedAnnotations();
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Name.class) != null)
                .peek(this::checkFieldType)
                .collect(toMap(f -> f.getDeclaredAnnotation(Name.class).value(), this::extractFieldValueViaReflection));
        //TODO here on version 6.17.2 getClass().getDeclaredFields() for SearchFieldMock we has elements, on version 6.18.0 we have null
    }

    private void checkFieldType(Field f) {
        if (!SelenideElement.class.isAssignableFrom(f.getType())
                && !Page.class.isAssignableFrom(f.getType())) {
            checkCollectionFieldType(f);
        }
    }

    private void checkCollectionFieldType(Field f) {
        if (ElementsCollection.class.isAssignableFrom(f.getType())) {
            return;
        } else if (List.class.isAssignableFrom(f.getType())) {
            ParameterizedType listType = (ParameterizedType) f.getGenericType();
            Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
            if (SelenideElement.class.isAssignableFrom(listClass) || Page.class.isAssignableFrom(listClass)) {
                return;
            }
        }
        throw new IllegalStateException(
                format("Поле с аннотацией @Name должно иметь тип SelenideElement или List<SelenideElement>.\n" +
                        "Если поле описывает блок, оно должно принадлежать классу, унаследованному от Page.\n" +
                        "Найдено поле с типом %s", f.getType()));
    }

    /**
     * Поиск по аннотации "Name"
     */
    private void checkNamedAnnotations() {
        List<String> list = Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Name.class) != null)
                .map(f -> f.getDeclaredAnnotation(Name.class).value())
                .collect(toList());
        if (list.size() != new HashSet<>(list).size()) {
            throw new IllegalStateException("Найдено несколько аннотаций @Name с одинаковым значением в классе " + this.getClass().getName());
        }
    }

    /**
     * Поиск и инициализация элементов страницы без аннотации Optional или Hidden
     */
    private List<SelenideElement> readWithWrappedElements() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Optional.class) == null && f.getDeclaredAnnotation(Hidden.class) == null)
                .map(this::extractFieldValueViaReflection)
                .flatMap(v -> v instanceof List ? ((List<?>) v).stream() : Stream.of(v))
                .map(Page::castToSelenideElement)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Поиск и инициализация элементов страницы c аннотацией Hidden
     */
    private List<SelenideElement> readWithHiddenElements() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getDeclaredAnnotation(Hidden.class) != null)
                .map(this::extractFieldValueViaReflection)
                .flatMap(v -> v instanceof List ? ((List<?>) v).stream() : Stream.of(v))
                .map(Page::castToSelenideElement)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * Поиск элементов страницы с типом ElementsCollection, не помеченных как Optional или Hidden
     */
    private List<ElementsCollection> readPrimaryElementsCollections() {
        return Arrays.stream(getClass().getDeclaredFields())
                .filter(f -> f.getType().equals(ElementsCollection.class))
                .filter(f -> f.getDeclaredAnnotation(Optional.class) == null && f.getDeclaredAnnotation(Hidden.class) == null)
                .map(f -> (ElementsCollection) Reflection.extractFieldValue(f, this))
                .collect(toList());
    }

    private Object extractFieldValueViaReflection(Field field) {
        return Reflection.extractFieldValue(field, this);
    }
}
