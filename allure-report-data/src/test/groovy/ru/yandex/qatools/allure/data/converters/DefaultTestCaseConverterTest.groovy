package ru.yandex.qatools.allure.data.converters

import org.junit.Test
import ru.yandex.qatools.allure.data.io.TestCaseReader
import ru.yandex.qatools.allure.model.Attachment
import ru.yandex.qatools.allure.model.Description
import ru.yandex.qatools.allure.model.DescriptionType
import ru.yandex.qatools.allure.model.Label
import ru.yandex.qatools.allure.model.LabelName
import ru.yandex.qatools.allure.model.Step
import ru.yandex.qatools.allure.model.TestCaseResult

import static ru.yandex.qatools.allure.config.AllureModelUtils.createSeverityLabel
import static ru.yandex.qatools.allure.model.SeverityLevel.CRITICAL
import static ru.yandex.qatools.allure.model.SeverityLevel.NORMAL
import static ru.yandex.qatools.allure.model.Status.PASSED

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 10.02.15
 */
class DefaultTestCaseConverterTest {

    def converter = new DefaultTestCaseConverter();

    @Test
    void shouldConvertEmptyTestCase() {
        def origin = new TestCaseResult(name: "name", status: PASSED)
        def modify = converter.convert(origin)
        assert modify
        assert modify.name == "name"
        assert modify.title == "Name"

        assert !modify.description
        assert !modify.failure

        assert modify.time
        assert modify.status == PASSED
        assert modify.severity == NORMAL
        assert modify.summary.steps == 0
        assert modify.summary.attachments == 0

        assert modify.attachments.empty
        assert modify.issues.empty
        assert modify.labels.empty
        assert modify.parameters.empty
        assert modify.steps.empty

        assert modify.suite
    }

    @Test
    void shouldCountTimeInTestCase() {
        def origin = new TestCaseResult(
                name: "name",
                start: 7,
                stop: 32
        )
        def modify = converter.convert(origin)

        assert modify.time.start == 7
        assert modify.time.stop == 32
        assert modify.time.duration == 25
    }

    @Test
    void shouldGetSeverityFromLabels() {
        def origin = new TestCaseResult(name: "name", labels: [createSeverityLabel(CRITICAL)])
        def modify = converter.convert(origin)

        use(DefaultTestCaseConverterTest) {
            modify.labels.checkLabel(LabelName.SEVERITY, CRITICAL.value())
        }
    }

    @Test
    void shouldHumanizeSuiteName() {
        def origin = new TestCaseResult(
                name: "name",
                labels: [new Label(name: TestCaseReader.SUITE_NAME, value: "suiteName")]
        )
        def modify = converter.convert(origin)

        assert modify.suite.name == "suiteName"
        assert modify.suite.title == "Suite name"
    }

    @Test
    void shouldGroupSuitesByName() {
        def firstOrigin = new TestCaseResult(
                name: "firstName",
                labels: [new Label(name: TestCaseReader.SUITE_NAME, value: "name")]
        )

        def secondOrigin = new TestCaseResult(
                name: "secondName",
                labels: [new Label(name: TestCaseReader.SUITE_NAME, value: "name")]
        )
        def firstModify = converter.convert(firstOrigin)
        def secondModify = converter.convert(secondOrigin)

        assert firstModify.suite.name == "name"
        assert secondModify.suite.name == "name"
        assert secondModify.suite.uid == firstModify.suite.uid
    }

    @Test
    void shouldConvertMarkdownDescription() {
        def origin = new TestCaseResult(
                name: "name",
                description: new Description(value: "**Hi**, *Charlie*", type: DescriptionType.MARKDOWN)
        )
        def modify = converter.convert(origin)

        assert modify.description
        assert modify.description.value == "<p><strong>Hi</strong>, <em>Charlie</em></p>"
        assert modify.description.type == DescriptionType.MARKDOWN
    }

    @Test
    void shouldKeepOtherTypesOfDescription() {
        def origin = new TestCaseResult(
                name: "name",
                description: new Description(value: "**Hi**, *Charlie*", type: DescriptionType.HTML)
        )
        def modify = converter.convert(origin)

        assert modify.description
        assert modify.description.value == "**Hi**, *Charlie*"
        assert modify.description.type == DescriptionType.HTML
    }

    @Test
    void shouldCountFirstLevelAttachments() {
        def origin = new TestCaseResult(
                name: "name",
                attachments: [new Attachment(), new Attachment(), new Attachment()]
        )
        def modify = converter.convert(origin)

        assert modify.summary.attachments == 3
    }

    @Test
    void shouldCountFirstLevelSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(), new Step(), new Step(), new Step()]
        )
        def modify = converter.convert(origin)

        assert modify.summary.steps == 4
    }

    @Test
    void shouldCountInnerSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(), new Step(steps: [new Step(), new Step(steps: [new Step()])]), new Step(), new Step()]
        )
        def modify = converter.convert(origin)

        assert modify.summary.steps == 7
    }

    @Test
    void shouldCountInnerStepsInSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(), new Step(steps: [new Step(), new Step(steps: [new Step()])]), new Step(), new Step()]
        )
        def modify = converter.convert(origin)

        assert modify.steps[1].summary.steps == 3
    }

    @Test
    void shouldCountInnerAttachments() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [
                        new Step(attachments: [new Attachment()]),
                        new Step(
                                steps: [
                                    new Step(),
                                    new Step(attachments: [new Attachment()]),
                                    new Step(),
                                    new Step()
                                ],
                                attachments: [
                                        new Attachment()
                                ])
                ]
        )
        def modify = converter.convert(origin)

        assert modify.summary.attachments == 3
    }

    @Test
    void shouldCountInnerAttachmentsInSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [
                        new Step(attachments: [new Attachment()]),
                        new Step(
                                steps: [
                                        new Step(),
                                        new Step(attachments: [new Attachment()]),
                                        new Step(),
                                        new Step()
                                ],
                                attachments: [
                                        new Attachment()
                                ])
                ]
        )
        def modify = converter.convert(origin)

        assert modify.steps[1].summary.attachments == 2
    }

    @Test
    void shouldConvertFirstLevelSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(name: "stepName")]
        )
        def modify = converter.convert(origin)

        assert modify.steps
        assert modify.steps.size() == 1
        assert modify.steps[0].name == "stepName"
    }

    @Test
    void shouldConvertInnerSteps() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(steps: [new Step(name: "stepName")])]
        )
        def modify = converter.convert(origin)

        assert modify.steps
        assert modify.steps.size() == 1
        assert modify.steps[0].steps
        assert modify.steps[0].steps.size() == 1
        assert modify.steps[0].steps[0].name == "stepName"
    }

    @Test
    void shouldHumanizeStepsTitle() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(name: "stepName")]
        )
        def modify = converter.convert(origin)

        assert modify.steps
        assert modify.steps.size() == 1
        assert modify.steps[0].title == "Step name"
    }

    @Test
    void shouldCountStepsTime() {
        def origin = new TestCaseResult(
                name: "name",
                steps: [new Step(start: 1, stop: 16)]
        )
        def modify = converter.convert(origin)

        assert modify.steps[0].time.start == 1
        assert modify.steps[0].time.stop == 16
        assert modify.steps[0].time.duration == 15
    }

    @Test
    void shouldConvertAttachments() {
        def origin = new TestCaseResult(
                name: "name",
                attachments: [new Attachment(title: "some")]
        )
        def modify = converter.convert(origin)

        assert modify.attachments[0].title == "some"
    }

    @Test
    void shouldGenerateUidForAttachments() {
        def origin = new TestCaseResult(
                name: "name",
                attachments: [new Attachment()]
        )
        def modify = converter.convert(origin)

        assert modify.attachments[0].uid
    }

    static def checkLabel(List<Label> labels, LabelName name, String expectedValue) {
        def found = labels.find { it.name == name.value() }
        assert found
        assert found.value == expectedValue
    }

}
