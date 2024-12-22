package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.magnariuk.lemonkasubstoolw.data.Classes.Project
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.magnariuk.lemonkasubstoolw.data.Classes.SRT
import com.magnariuk.lemonkasubstoolw.data.Classes.Actor
import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.api.subs.SRTParser
import com.magnariuk.lemonkasubstoolw.data.util.*
import com.magnariuk.lemonkasubstoolw.data.api.subs.SRTCounter
import com.magnariuk.lemonkasubstoolw.data.api.CacheController
import com.magnariuk.lemonkasubstoolw.data.api.subs.ASSCounter
import com.magnariuk.lemonkasubstoolw.data.api.subs.ParserIS
import com.magnariuk.lemonkasubstoolw.data.util.showError
import com.magnariuk.lemonkasubstoolw.front.util.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import java.io.File

@Route("/tools/calculator", layout = MainLayout::class)
@PageTitle("Рахівниця")
class ActorMeter: KComposite(), BeforeEnterObserver {
    private lateinit var dynamicLayout: VerticalLayout
    private var srt: MutableList<SRT> = mutableListOf()
    private var ass: MutableList<Ass> = mutableListOf()
    private val cacheController = CacheController()
    private var selectedProject: Project? = null
    private var selectedActor: Actor? = null

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
        if(srt == null || selectedProject == null || selectedActor == null) {
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

            val actorSelector = ComboBox<Actor>().apply {
                label = "Оберіть актора"
                setItemLabelGenerator { it.actorName }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val projectSelector = ComboBox<Project>().apply {
                label = "Оберіть проєкт"
                setItems(cacheController.getCache()!!.projects)
                setItemLabelGenerator { it.name }

                addValueChangeListener { value ->
                    actorSelector.setItems(value.value.selected)
                }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val chooseSubHolder = VerticalLayout(
                HorizontalLayout(uploader, VerticalLayout(projectSelector, actorSelector)).apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                }
                .apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                }, Button("Підтвердити").apply {
                    addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                    onLeftClick {
                        if(srt.size >=1 || ass.size >=1) {
                            if(projectSelector.value != null) {
                                if(actorSelector.value != null) {
                                    selectedProject = projectSelector.value
                                    selectedActor = actorSelector.value

                                    val dialog = Dialog().apply {
                                        headerTitle = "Пораховано"
                                        val counters = srt.map { SRTCounter(it) }
                                        val assCounters = ass.map { ASSCounter(it) }

                                        var allCount = 0
                                        var allDialogs = 0
                                        var allNumbers = 0

                                        val numbers = TextArea().apply {
                                            width = 500.px
                                            isReadOnly = true
                                            this.value = ""
                                        }
                                        numbers.value+="Числа рекомендується перевірити вручну!\n"
                                        counters.forEach { counter ->
                                            val n = counter.count()
                                            val r = counter.count_dialogs()
                                            allCount+= n["words"]!!.toInt()
                                            allDialogs+=r


                                            val nums = n.get("numbers")!!.split(" ")
                                            val counts = n.get("numbers_counted")!!.split(" ")
                                            numbers.value+=counter.srt.subName +"\n"
                                            numbers.value+="Слова: ${n["words"]} \n"

                                            numbers.value+="Числа:\n"
                                            for (i in nums.indices) {
                                                numbers.value+="   ${nums[i]} = ${counts[i]}\n"
                                            }
                                            if(n.get("numbers")!=""){
                                                val all = counts.map { it.toInt() }.reduce(Int::plus)
                                                allNumbers += all
                                                numbers.value+= "Загалом: ${all}\n\n"
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

                                            numbers.value+="Числа:\n"
                                            for (i in nums.indices) {
                                                numbers.value+="   ${nums[i]} = ${counts[i]}\n"
                                            }
                                            if(n.get("numbers")!=""){
                                                val all = counts.map { it.toInt() }.reduce(Int::plus)
                                                allNumbers += all
                                                numbers.value+= "Загалом: ${all}\n\n"
                                            }

                                        }

                                        numbers.value+="Загалом числа: $allNumbers\n"
                                        numbers.value+="Загалом разом зі словами: ${allCount+allNumbers}\n"

                                        add( VerticalLayout(
                                            NativeLabel("Репліки: $allDialogs"),
                                            NativeLabel("Слова: $allCount"),
                                            numbers,
                                        ) )

                                    }
                                    dialog.open()

                                } else{
                                    showError("Оберіть актора")
                                }
                            } else {
                                showError("Оберіть проєкт")
                            }
                        } else {
                            showError("'Завантаж'те субтитри")
                        }

                    }
                }
            ).apply {
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }
            dynamicLayout.add(chooseSubHolder)
        }
    }

    override fun beforeEnter(p0: BeforeEnterEvent?) {
        updateUI()
    }
}