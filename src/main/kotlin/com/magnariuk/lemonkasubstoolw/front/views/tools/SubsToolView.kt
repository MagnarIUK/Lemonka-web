package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.magnariuk.lemonkasubstoolw.data.Classes.Actor
import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.Classes.Project
import com.magnariuk.lemonkasubstoolw.data.api.CacheController
import com.magnariuk.lemonkasubstoolw.data.api.subs.ParserIS
import com.magnariuk.lemonkasubstoolw.data.util.*
import com.magnariuk.lemonkasubstoolw.data.util.enum.SubTypes
import com.magnariuk.lemonkasubstoolw.front.util.MainLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.NativeLabel
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.data.provider.ListDataProvider
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.*
import com.vaadin.flow.server.StreamResource
import java.io.File
import org.vaadin.olli.FileDownloadWrapper

@PageTitle("Субтитри")
@Route("/tools/sub", layout = MainLayout::class)
class SubsToolView: KComposite(), BeforeEnterObserver {
    private var cacheController = CacheController()
    private lateinit var dynamicLayout: VerticalLayout
    private var ass: Ass? = null
    private var hideSelected = cacheController.getCache()!!.hideSelected
    private var currentProject: Project? = null

    override fun beforeEnter(p0: BeforeEnterEvent?) {
        updateUI()
    }

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

        if(ass == null || currentProject == null) {
            val buffer = MemoryBuffer()

            val uploader = Upload(buffer).apply {
                i18n = UploadUkrainian18()

                maxFiles=1

                addSucceededListener { event ->
                    val fileName = event.fileName
                    val inputStream = buffer.inputStream
                    if(File(fileName).extension != "ass"){
                        showError("Необхідно файл .ass")
                    } else {
                        ass = ParserIS().parseAssFile(inputStream, fileName)
                    }
                }
            }
            val projectSelector = ComboBox<Project>().apply {
                label = "Оберіть проєкт"
                setItems(cacheController.getCache()!!.projects + Project("Додати новий"))
                setItemLabelGenerator { it.name }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val chooseSubHolder = VerticalLayout(HorizontalLayout(uploader, projectSelector)
                .apply {
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }, Button("Підтвердити").apply {
                onLeftClick {
                    if(ass != null) {
                        if(projectSelector.value != null) {
                            if (projectSelector.value.name == "Додати новий") {
                                val dialog = Dialog().apply {
                                    width = 25.p
                                    height = 30.p
                                    headerTitle = "Новий проєкт"
                                    val projectNameField = TextField().apply {
                                        placeholder = "Введіть назву проєкту"
                                    }
                                    add(VerticalLayout(projectNameField,
                                        HorizontalLayout(
                                            Button("Скасувати").apply {
                                                onLeftClick { close() }
                                            },
                                            Button("Підтвердити").apply {
                                                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                                onLeftClick {
                                                    val c = cacheController.getCache()!!
                                                    val newProject = Project(projectNameField.value.trim())
                                                    if(c.projects.find { it.name == newProject.name } == null) {
                                                        val assActors = ass!!.getAllActors().distinct().toMutableList()
                                                        assActors.forEach { character ->
                                                            val splitCharacters = character.split("and", ",").map { it.trim() }
                                                            splitCharacters.forEach {
                                                                if(!newProject.characters.contains(it)){
                                                                    newProject.characters.add(it)
                                                                }
                                                            }
                                                        }

                                                        c.projects.add(newProject)
                                                        cacheController.saveCache(c)
                                                        currentProject = newProject
                                                        updateUI()
                                                        close()
                                                    } else {
                                                        showError("Проєкт з таким ім'ям вже існує")
                                                    }

                                                }
                                            }
                                        ).apply {
                                            alignItems = Alignment.CENTER
                                            justifyContentMode = JustifyContentMode.CENTER

                                        }
                                    ).apply {
                                        alignItems = Alignment.CENTER
                                        justifyContentMode = JustifyContentMode.CENTER
                                    })
                                }
                                dialog.open()

                            } else {
                                val c = cacheController.getCache()!!
                                val projectX = c.projects.find { it.name == projectSelector.value.name }!!
                                val assActors = ass!!.getAllActors().distinct().toMutableList()
                                assActors.forEach { character ->
                                    val splitCharacters = character.split("and", ",").map { it.trim() }
                                    splitCharacters.forEach {
                                        if(!projectX.characters.contains(it)){
                                            projectX.characters.add(it)
                                        }
                                    }
                                }
                                cacheController.saveCache(c)
                                currentProject =  cacheController.getCache()!!.projects.find { it.name == projectSelector.value.name }
                                updateUI()
                            }
                        } else {
                            showError("Ви не обрали проєкт, якщо проєкт новий — оберіть \"Додати новий\"")
                        }
                    } else {
                        showError("Завантажте коректні субтитри")
                    }
                }
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            }).apply {
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }
            dynamicLayout.add(chooseSubHolder)
        } else {
            val actorsDataProvider = ListDataProvider(cacheController.getCache()!!.actors)
            val actorsSearchBox = TextField().apply {
                placeholder = "Актор..."

                addValueChangeListener { event ->
                    val filterText = event.value ?: ""
                    actorsDataProvider.setFilter { actor ->
                        actor.contains(filterText, true)
                    }
                }
            }
            val addActorButton = Button().apply {
                icon = Icon(VaadinIcon.FILE_ADD)
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                onLeftClick {
                    if(!cacheController.getCache()!!.actors.contains(actorsSearchBox.value.trim())){
                        val cacheOld = cacheController.getCache()!!
                        cacheOld.actors.add(actorsSearchBox.value.trim())
                        cacheController.saveCache(cacheOld)
                        actorsDataProvider.items.add(actorsSearchBox.value.trim())
                        updateUI()
                    }
                }
            }
            val actorsGrid = Grid<String>().apply {
                width = 400.px
                height = 600.px
                style.set(CSS.BORDER, ELEMENT().add(1.px).add(CSS.SOLID).add("d3d3d3".hex).css())
                style.set(CSS.BORDER_RADIUS, 10.px)


                dataProvider = actorsDataProvider
                addColumn {it}.setHeader("Актор")

                addColumn(ComponentRenderer {actorX ->
                    val addButton = Button().apply {
                        val c = cacheController.getCache()!!
                        val project = c.projects.find { it.name == currentProject!!.name }!!
                        if (project.actors.contains(actorX)) {
                            icon = Icon(VaadinIcon.MINUS)
                            onLeftClick {
                                if(project.actors.contains(actorX)) {
                                    val actorToRemove = project.selected.find { it.actorName == actorX }!!
                                    project.selected.remove(actorToRemove)
                                    project.actors.remove(actorX)
                                }
                                cacheController.saveCache(c)
                                currentProject = project
                                updateUI()
                            }
                        } else {
                            icon = Icon(VaadinIcon.PLUS)
                            onLeftClick {
                                if(!project.actors.contains(actorX)) {
                                    project.selected.add(Actor(actorX))
                                    project.actors.add(actorX)
                                }
                                cacheController.saveCache(c)
                                currentProject = project
                                updateUI()
                            }
                        }

                        addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_PRIMARY)

                    }
                    val deleteButton = Button().apply {
                        icon = Icon(VaadinIcon.TRASH)
                        addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE)
                        onLeftClick {
                            val c = cacheController.getCache()!!
                            c.actors.remove(actorX)
                            actorsDataProvider.items.remove(actorX)
                            cacheController.saveCache(c)
                            updateUI()
                        }
                    }

                    HorizontalLayout(deleteButton, addButton)
                })
            }



            val leftBar = VerticalLayout(
                HorizontalLayout(
                    actorsSearchBox, addActorButton
                ).apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                },
                actorsGrid
            ).apply {
                width = 600.px
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }

            val addedCharactersDataProvider = ListDataProvider(cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.selected)

            val charactersSelectorActorSearchBox = TextField().apply {
                placeholder = "Пошук..."

                addValueChangeListener { event ->
                    val filterText = event.value ?: ""
                    addedCharactersDataProvider.setFilter { actor ->
                        actor.actorName.contains(filterText, true)
                    }
                }
            }
            val hideSelectedCharacters = Checkbox("Ховати вибраних персонажів").apply {
                value = hideSelected
                addValueChangeListener { event ->
                    val newValue = event.value
                    if (hideSelected != newValue) {
                        hideSelected = newValue
                        updateUI()
                    }
                }
            }

            val charactersGrid = Grid<Actor>().apply {
                width = 700.px
                height = 600.px
                style.set(CSS.BORDER, ELEMENT().add(1.px).add(CSS.SOLID).add("d3d3d3".hex).css())
                style.set(CSS.BORDER_RADIUS, 10.px)
                dataProvider = addedCharactersDataProvider
                addColumn {it.actorName}.setHeader("Актор")
                addColumn(ComponentRenderer {actorX ->

                    val characterChooser = MultiSelectComboBox<String>().apply {
                        /*if(hideSelected){
                            setItems(
                                cacheController.getCache()!!.projects
                                    .find { it.name == currentProject?.name }
                                    ?.characters
                                    ?.filter { character ->
                                        val selectedActor = c.projects
                                            .find { it.name == currentProject?.name }
                                            ?.selected
                                            ?.find { it.actorName == actorX.actorName }

                                        val otherActorSelected = c.projects
                                            .find { it.name == currentProject?.name }
                                            ?.selected
                                            ?.find { it.actorName != actorX.actorName }

                                        selectedActor?.characterNames?.contains(character) == true ||
                                                otherActorSelected?.characterNames?.contains(character) != true
                                    }
                            )

                        } else{
                            setItems(cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.characters)
                        }*/
                        setItems(cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.characters)
                        setItemLabelGenerator { it }
                        value = actorX.characterNames.toSet()

                        addValueChangeListener { event ->
                            val c = cacheController.getCache()!!
                            val cProject = c.projects.find { it.name == currentProject!!.name }!!.selected!!
                            val cCharacters = cProject.find { it.actorName == actorX.actorName }!!
                            cCharacters.characterNames.clear()
                            cCharacters.characterNames.addAll(event.value)
                            cacheController.saveCache(c)
                        }
                    }

                    HorizontalLayout(
                        characterChooser,
                    ).apply {

                    }
                })

            }


            val selectFormat = ComboBox<SubTypes>().apply {
                setItems(SubTypes.entries)
                setItemLabelGenerator { it.value }
                isRequired = true
                isRequiredIndicatorVisible = true
            }

            val createButton = Button("Створити субтитри").apply {
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                onLeftClick {
                    val createdFiles: MutableMap<String, StreamResource> = mutableMapOf()
                    val projectX = cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.selected
                    val assParser = ParserIS()

                    if(selectFormat.value != null){
                        when (selectFormat.value.format){
                            "srt" -> {
                                projectX.forEach { actor ->
                                    val trier = assParser.createSubRip("${actor.actorName}.srt", ass!!, actor.characterNames)
                                    if(trier != null){
                                        createdFiles["${actor.actorName}.srt"] = trier
                                    }
                                }
                                val dialog = Dialog().apply {
                                    headerTitle = "Завантажити готові файли"
                                    val files = VerticalLayout().apply {}
                                    createdFiles.forEach { file ->
                                        val downloadButton = Button().apply {
                                            icon = Icon(VaadinIcon.DOWNLOAD)
                                            addThemeVariants(ButtonVariant.LUMO_ICON)
                                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                        }
                                        val downloadWrapper = FileDownloadWrapper(file.value)
                                        downloadWrapper.wrapComponent(downloadButton)

                                        files.add(
                                            HorizontalLayout(NativeLabel(file.key), downloadWrapper)
                                        )
                                    }
                                    add(files)
                                }
                                dialog.open()
                            }
                            "ass" -> {
                                projectX.forEach { actor ->
                                    val trier = assParser.createAss("${actor.actorName}.ass", ass!!, actor.characterNames)
                                    if(trier != null){
                                        createdFiles["${actor.actorName}.ass"] = trier
                                    }
                                }
                                val dialog = Dialog().apply {
                                    headerTitle = "Завантажити готові файли"
                                    val files = VerticalLayout().apply {}
                                    createdFiles.forEach { file ->
                                        val downloadButton = Button().apply {
                                            icon = Icon(VaadinIcon.DOWNLOAD)
                                            addThemeVariants(ButtonVariant.LUMO_ICON)
                                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                        }
                                        val downloadWrapper = FileDownloadWrapper(file.value)
                                        downloadWrapper.wrapComponent(downloadButton)

                                        files.add(
                                            HorizontalLayout(NativeLabel(file.key), downloadWrapper)
                                        )
                                    }
                                    add(files)
                                }
                                dialog.open()
                            }
                        }




                    } else{
                        showError("Оберіть формат субтитрів")
                    }


                }
            }
            val separateButton = Button("Розділити по персонажам").apply {
                addThemeVariants(ButtonVariant.LUMO_TERTIARY)
                onLeftClick {
                    val createdFiles: MutableMap<String, StreamResource> = mutableMapOf()
                    val projectX = cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.characters
                    val assParser = ParserIS()

                    if(selectFormat.value != null){
                        when (selectFormat.value.format){
                            "srt" -> {
                                projectX.forEach { actor ->
                                    val trier = assParser.createSubRip("${actor}.srt", ass!!, listOf(actor))
                                    if(trier != null){
                                        createdFiles["${actor}.srt"] = trier
                                    }
                                }
                                val dialog = Dialog().apply {
                                    headerTitle = "Завантажити готові файли"
                                    val files = VerticalLayout().apply {}
                                    createdFiles.forEach { file ->
                                        val downloadButton = Button().apply {
                                            icon = Icon(VaadinIcon.DOWNLOAD)
                                            addThemeVariants(ButtonVariant.LUMO_ICON)
                                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                        }
                                        val downloadWrapper = FileDownloadWrapper(file.value)
                                        downloadWrapper.wrapComponent(downloadButton)

                                        files.add(
                                            HorizontalLayout(NativeLabel(file.key), downloadWrapper)
                                        )
                                    }
                                    add(files)
                                }
                                dialog.open()
                            }
                            "ass" -> {
                                projectX.forEach { actor ->
                                    val trier = assParser.createAss("${actor}.ass", ass!!, listOf(actor))
                                    if(trier != null){
                                        createdFiles["${actor}.ass"] = trier
                                    }
                                }
                                val dialog = Dialog().apply {
                                    headerTitle = "Завантажити готові файли"
                                    val files = VerticalLayout().apply {}
                                    createdFiles.forEach { file ->
                                        val downloadButton = Button().apply {
                                            icon = Icon(VaadinIcon.DOWNLOAD)
                                            addThemeVariants(ButtonVariant.LUMO_ICON)
                                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                        }
                                        val downloadWrapper = FileDownloadWrapper(file.value)
                                        downloadWrapper.wrapComponent(downloadButton)

                                        files.add(
                                            HorizontalLayout(NativeLabel(file.key), downloadWrapper)
                                        )
                                    }
                                    add(files)
                                }
                                dialog.open()
                            }
                        }




                    } else{
                        showError("Оберіть формат субтитрів")
                    }
                }
            }


            val rightBar = VerticalLayout(
                HorizontalLayout(Button().apply {
                    icon = Icon(VaadinIcon.REFRESH)
                    addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE)
                    onLeftClick {
                        updateUI()
                    }
                },charactersSelectorActorSearchBox, hideSelectedCharacters),
                charactersGrid,
                HorizontalLayout(selectFormat,createButton, separateButton).apply {
                    alignItems = Alignment.CENTER
                    justifyContentMode = JustifyContentMode.CENTER
                }
            ).apply {
                width = 800.px
                alignItems = Alignment.CENTER
                justifyContentMode = JustifyContentMode.CENTER
            }



            val menuHolder = HorizontalLayout(leftBar, rightBar).apply {
                alignItems = Alignment.START
                justifyContentMode = JustifyContentMode.CENTER
            }
            dynamicLayout.add(menuHolder)
        }


    }

}