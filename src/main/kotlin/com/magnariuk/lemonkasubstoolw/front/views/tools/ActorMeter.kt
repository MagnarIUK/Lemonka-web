package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.magnariuk.lemonkasubstoolw.data.api.*
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.magnariuk.lemonkasubstoolw.data.Classes.SRT
import com.magnariuk.lemonkasubstoolw.data.Classes.Actor
import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.api.database.ApiService
import com.magnariuk.lemonkasubstoolw.data.api.database.AuthService
import com.magnariuk.lemonkasubstoolw.data.api.database.Character
import com.magnariuk.lemonkasubstoolw.data.api.database.Project
import com.magnariuk.lemonkasubstoolw.data.api.subs.SRTParser
import com.magnariuk.lemonkasubstoolw.data.util.*
import com.magnariuk.lemonkasubstoolw.data.api.subs.SRTCounter
import com.magnariuk.lemonkasubstoolw.data.api.subs.ASSCounter
import com.magnariuk.lemonkasubstoolw.data.api.subs.ParserIS
import com.magnariuk.lemonkasubstoolw.data.util.showError
import com.magnariuk.lemonkasubstoolw.front.util.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.beans.factory.annotation.Autowired
import org.vaadin.olli.FileDownloadWrapper
import java.awt.Checkbox
import java.io.File
import kotlin.reflect.jvm.internal.impl.util.Check

@Route("/tools/calculator", layout = MainLayout::class)
@PageTitle("Рахівниця")
class ActorMeter(
    @Autowired private val api: ApiService,
    @Autowired private val authService: AuthService
): KComposite(), BeforeEnterObserver {
    private lateinit var dynamicLayout: VerticalLayout
    private var srt: MutableList<SRT> = mutableListOf()
    private var ass: MutableList<Ass> = mutableListOf()
    private var selectedProject: Project? = null
    private var selectedCharacters: MutableList<Character> = mutableListOf()
    private var user = authService.getLoggedInUser()

    private val root = ui {
        verticalLayout {
            alignItems = FlexComponent.Alignment.CENTER
            justifyContentMode = FlexComponent.JustifyContentMode.START

            dynamicLayout = verticalLayout {
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }
        }
    }

    private fun updateUI() {
        dynamicLayout.removeAll()
        if(srt == null || selectedProject == null || selectedCharacters.size >=1) {
            val buffer = MultiFileMemoryBuffer()
            val uploader = Upload(buffer).apply {
                i18n = UploadUkrainian18()
                addSucceededListener { event ->
                    val fileName = event.fileName
                    val inputStream = buffer.getInputStream(fileName)
                    val extension = File(fileName).extension
                    when (extension) {
                        "srt" -> srt.add(SRTParser(inputStream, fileName).parse())
                        "ass" -> ass.add(ParserIS().parseAssFile(inputStream, fileName))
                        else -> showError("Потрібен формат srt або ass")
                    }
                }

                addFileRemovedListener { event ->
                    srt.removeAll { it.subName.toString() == event.fileName.toString() }
                    ass.removeAll { it.subName == event.fileName.toString() }
                }
            }

            val countAllCharacters = com.vaadin.flow.component.checkbox.Checkbox("Врахувати всіх персонажів у субтитрах")

            val actorSelector = MultiSelectComboBox<Character>().apply {
                label = "Оберіть персонажів"
                setItemLabelGenerator { it.name }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val projectSelector = ComboBox<Project>().apply {
                label = "Оберіть проєкт"
                setItems(api.getProjects(user))
                setItemLabelGenerator { it.name }

                addValueChangeListener { value ->
                    actorSelector.setItems(api.getProjectCharacters(value.value.id))
                }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val chooseSubHolder = VerticalLayout(
                HorizontalLayout(uploader, VerticalLayout(
                    projectSelector,
                    countAllCharacters ,
                    actorSelector,
                    Button("Підтвердити").apply {
                        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                        onLeftClick {
                            if(srt.size >=1 || ass.size >=1) {
                                if(projectSelector.value != null) {
                                    if(actorSelector.value != null) {
                                        selectedProject = projectSelector.value
                                        selectedCharacters = actorSelector.value.toMutableList()

                                        val dialog = Dialog().apply {
                                            headerTitle = "Пораховано"
                                            var counters: List<SRTCounter>
                                            var assCounters: List<ASSCounter>
                                            if(countAllCharacters.value) {
                                                counters = srt.map { SRTCounter(it) }
                                                assCounters = ass.map { ASSCounter(it) }
                                            } else{
                                                counters = srt.map { SRTCounter(it, selectedCharacters.map { it.name }.toMutableList()) }
                                                assCounters = ass.map { ASSCounter(it, selectedCharacters.map { it.name }.toMutableList()) }
                                            }


                                            var allCount = 0
                                            var allDialogs = 0
                                            var allNumbers = 0

                                            val numbers = TextArea().apply {
                                                width = 500.px
                                                isReadOnly = true
                                                this.value = ""
                                            }
                                            numbers.value+=selectedCharacters.joinToString(" | ")+"\n"
                                            numbers.value+="Числа рекомендується перевірити вручну!\n"

                                            counters.forEach { counter ->
                                                val n = counter.count(api.getSeparators().map { it.separator })
                                                val r = counter.count_dialogs()
                                                allCount+= n["words"]!!.toInt()
                                                allDialogs+=r


                                                val nums = n.get("numbers")!!.split(" ")
                                                val counts = n.get("numbers_counted")!!.split(" ")
                                                numbers.value+=counter.srt.subName +"\n"
                                                numbers.value+="Слова: ${n["words"]} \n"

                                                if(n.get("numbers")!=""){
                                                    numbers.value+="Числа:\n"
                                                    for (i in nums.indices) {
                                                        numbers.value+="   ${nums[i]} = ${counts[i]}\n"
                                                    }
                                                    val all = counts.map { it.toInt() }.reduce(Int::plus)
                                                    allNumbers += all
                                                    numbers.value+= "Загалом: ${all}\n\n"
                                                }else{
                                                    numbers.value+="\n"
                                                }

                                            }

                                            assCounters.forEach { counter ->
                                                val n = counter.count()
                                                val r = counter.count_dialogs()
                                                allCount+= n["words"]!!.toInt()
                                                allDialogs+=r
                                                val nums = n.get("numbers")!!.split(" ")
                                                val counts = n.get("numbers_counted")!!.split(" ")
                                                numbers.value+=counter.ass.subName +"\n"
                                                numbers.value+="Слова: ${n["words"]} \n"

                                                if(n.get("numbers")!=""){
                                                    val all = counts.map { it.toInt() }.reduce(Int::plus)
                                                    numbers.value+="Разом з числами: ${n["words"]!!.toInt()+all}\n"

                                                    numbers.value+="Числа:\n"
                                                    for (i in nums.indices) {
                                                        numbers.value+="   ${nums[i]} = ${counts[i]}\n"
                                                    }
                                                    allNumbers += all
                                                    numbers.value+= "Загалом: ${all}\n\n"
                                                } else{
                                                    numbers.value+="\n"
                                                }

                                            }

                                            numbers.value+="Загалом числа: $allNumbers\n"
                                            numbers.value+="Загалом числа разом зі словами: ${allCount+allNumbers}\n"
                                            val save = numbers.value
                                            numbers.value = "Репліки: $allDialogs\nСлова: $allCount\n\n$save"

                                            val downloadButton = Button().apply {
                                                icon = Icon(VaadinIcon.DOWNLOAD)
                                                addThemeVariants(ButtonVariant.LUMO_ICON)
                                                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                            }
                                            val downloadWrapper = FileDownloadWrapper(write_to_txt(numbers.value))
                                            downloadWrapper.wrapComponent(downloadButton)

                                            add( VerticalLayout(
                                                downloadWrapper,
                                                numbers,
                                            ) )

                                        }
                                        dialog.open()

                                    } else{
                                        showError("Оберіть персонажів")
                                    }
                                } else {
                                    showError("Оберіть проєкт")
                                }
                            } else {
                                showError("'Завантаж'те субтитри")
                            }

                        }
                    }
                    )).apply {
                    alignItems = Alignment.START
                    justifyContentMode = JustifyContentMode.CENTER
                }
                .apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                },
            ).apply {
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }
            dynamicLayout.add(chooseSubHolder)
        }
        if(user == null){
            showError("Ви не автентифіковані")
            val dialog = Dialog().apply{

                val usernameField = TextField("Ім'я користувача").apply {
                    width = 300.px
                    isRequired = true
                    isRequiredIndicatorVisible = true
                }
                val passwordField = TextField("Пароль").apply {
                    width = 300.px
                    isRequired = true
                    isRequiredIndicatorVisible = true
                }

                val logIn = Button("Увійти").apply {
                    setWidth(200.px)
                    addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                    addClickListener {
                        val _username = usernameField.value.trim()
                        val _password = passwordField.value.trim()
                        val t = authService.login(_username, _password)
                        when (t) {
                            "s" -> {
                                showSuccess("Успішний вхід")
                                user = authService.getLoggedInUser()
                                close()
                                updateUI()
                            }
                            "e:pnv" -> {
                                passwordField.isInvalid = true
                                passwordField.errorMessage = "невірний пароль"
                            }
                            "e:unf" -> {
                                usernameField.isInvalid = true
                                usernameField.errorMessage = "користувача не знайдено"
                            }
                        }
                    }
                }
                val signUp = Button("Зареєструватися").apply {
                    setWidth(200.px)
                    addClickListener {
                        val _username = usernameField.value.trim()
                        val _password = passwordField.value.trim()
                        if(_username.length < 4) {
                            usernameField.isInvalid = true
                            usernameField.errorMessage = "ім'я користувача занадто коротке"
                        } else if(_password.length < 8) {
                            passwordField.isInvalid = true
                            passwordField.errorMessage = "пароль занадто короткий"
                        } else {

                            val r = authService.register(_username, _password)
                            when (r) {
                                "e:uax" -> {
                                    usernameField.isInvalid = true
                                    usernameField.errorMessage = "користувач з таким ім'ям вже існує"
                                }

                                "s" -> {
                                    showSuccess("Успішна реєстрація")
                                    user = authService.getLoggedInUser()
                                    close()
                                    updateUI()
                                }
                            }
                        }
                    }
                }
                isCloseOnOutsideClick = false
                isCloseOnEsc = false
                add(VerticalLayout(usernameField, passwordField, logIn, signUp).apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                })
                open()
            }
        }
    }

    override fun beforeEnter(p0: BeforeEnterEvent?) {
        updateUI()
    }
}