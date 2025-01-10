package com.magnariuk.lemonkasubstoolw.front.views.tools

import com.github.mvysny.karibudsl.v10.KComposite
import com.github.mvysny.karibudsl.v10.onLeftClick
import com.github.mvysny.karibudsl.v10.verticalLayout
import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.api.database.*
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
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import org.vaadin.olli.FileDownloadWrapper

@PageTitle("Субтитри")
@Route("/tools/sub", layout = MainLayout::class)
class SubsToolView(
    @Autowired private val api: ApiService
): KComposite(), BeforeEnterObserver {
    private lateinit var dynamicLayout: VerticalLayout
    private var ass: Ass? = null
    private var hideSelected = api.getSettings().hideSelected
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
                setItems(api.getProjects() + Project(1000000, "Додати новий"))
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
                            if (projectSelector.value.id == 1000000) {
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
                                                    if(api.getProjectByName(projectNameField.value.trim()) == null) {
                                                        val newProject = api.getProject(api.createProject(projectNameField.value.trim()))
                                                        currentProject = newProject
                                                        val assActors = ass!!.getAllActors().distinct().toMutableList()
                                                        assActors.forEach { character ->
                                                            val separs = api.getSeparators().map { it.separator }
                                                            val splitCharacters = character.split(*separs.toTypedArray()).map { it.trim() }
                                                            splitCharacters.forEach {
                                                                if(api.getCharacterByName(it, currentProject!!.id) == null){
                                                                    api.addCharacter(it, newProject.id)
                                                                }
                                                            }
                                                        }
                                                        close()
                                                        updateUI()
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

                                val projectX = api.getProject(projectSelector.value.id)
                                val assActors = ass!!.getAllActors().distinct().toMutableList()
                                assActors.forEach { character ->
                                    val separs = api.getSeparators().map { it.separator }
                                    val splitCharacters = character.split(*separs.toTypedArray()).map { it.trim() }
                                    splitCharacters.forEach {
                                        if(api.getCharacterByName(it, projectX.id) == null){
                                            api.addCharacter(it, projectX.id)
                                        }
                                    }
                                }
                                currentProject = projectX
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
            val actorsDataProvider = ListDataProvider(api.getActors())
            val actorsSearchBox = TextField().apply {
                placeholder = "Актор..."

                addValueChangeListener { event ->
                    val filterText = event.value ?: ""
                    actorsDataProvider.setFilter { actor ->
                        actor.actorName.contains(filterText, true)
                    }
                }
            }
            val addActorButton = Button().apply {
                icon = Icon(VaadinIcon.FILE_ADD)
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                onLeftClick {
                    if(api.getActorByName(actorsSearchBox.value.trim()) == null){
                        api.addActor(actorsSearchBox.value.trim())
                        actorsDataProvider.items.clear()
                        actorsDataProvider.items.addAll(api.getActors())
                        updateUI()
                    }
                }
            }
            val actorsGrid = Grid<Actor>().apply {
                width = 400.px
                height = 600.px
                style.set(CSS.BORDER, ELEMENT().add(1.px).add(CSS.SOLID).add("d3d3d3".hex).css())
                style.set(CSS.BORDER_RADIUS, 10.px)


                dataProvider = actorsDataProvider
                addColumn {it.actorName}.setHeader("Актор")

                addColumn(ComponentRenderer {actorX ->
                    val addButton = Button().apply {
                        val assign = api.getProjectAssignmentsByActor(currentProject!!.id, actorX.id)
                        if (assign != null) {
                            icon = Icon(VaadinIcon.MINUS)
                            onLeftClick {
                                api.removeAssignment(assign.id)
                                updateUI()
                            }
                        } else {
                            icon = Icon(VaadinIcon.PLUS)
                            onLeftClick {
                                api.addAssignment(actorX.id, currentProject!!.id)
                                updateUI()
                            }
                        }

                        addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_PRIMARY)
                    }
                    val deleteButton = Button().apply {
                        icon = Icon(VaadinIcon.TRASH)
                        addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE)
                        onLeftClick {
                            api.removeActor(actorX.id)
                            actorsDataProvider.items.remove(actorX)
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

            //cacheController.getCache()!!.projects.find { it.name == currentProject!!.name }!!.selected
            val assignedActorsDataProvider = ListDataProvider(api.getProjectAssignments(currentProject!!.id))

            val charactersSelectorActorSearchBox = TextField().apply {
                placeholder = "Пошук..."

                addValueChangeListener { event ->
                    val filterText = event.value ?: ""
                    assignedActorsDataProvider.setFilter { actor ->
                        api.getActor(actor.actor).actorName.contains(filterText, true)
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

            val charactersGrid = Grid<Assignment>().apply {
                width = 700.px
                height = 600.px
                style.set(CSS.BORDER, ELEMENT().add(1.px).add(CSS.SOLID).add("d3d3d3".hex).css())
                style.set(CSS.BORDER_RADIUS, 10.px)
                dataProvider = assignedActorsDataProvider
                addColumn {api.getActor(it.actor).actorName}.setHeader("Актор")
                addColumn(ComponentRenderer {assignment ->

                    val characterChooser = MultiSelectComboBox<Character>().apply {
                        if(hideSelected){
                    setItems(api.getProjectCharacters(currentProject!!.id).filter { it.actor == null || it.actor == assignment.actor })
                        } else{
                            setItems(api.getProjectCharacters(currentProject!!.id))
                        }
                        setItemLabelGenerator { it.name }
                        value = api.getCharactersByActor(assignment.actor, currentProject!!.id).toSet()

                        addValueChangeListener { event ->
                            val new = event.value
                            val old = event.oldValue

                            val addedValues = new.minus(old)
                            val removedValues = old.minus(new)

                            addedValues.forEach {
                                api.assignActorToCharacter(assignment.actor, it.id)
                            }
                            removedValues.forEach {
                                api.assignActorToCharacter(null, it.id)
                            }
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

            val renameButton = Button("Перейменувати акторів").apply {
                onLeftClick {
                    val actors: MutableList<com.magnariuk.lemonkasubstoolw.data.Classes.Actor> = mutableListOf()
                    api.getProjectAssignments(currentProject!!.id).forEach { assignment ->
                        val actor = api.getActor(assignment.actor)
                        val characters = api.getCharactersByActor(actor.id, currentProject!!.id)
                        actors.add(com.magnariuk.lemonkasubstoolw.data.Classes.Actor(actor.actorName, characters.map { it.name }.toMutableList()))
                    }

                    val assParser = ParserIS()
                    val separs = api.getSeparators().map { it.separator }
                    val file = assParser.renameActors(ass!!, actors, separs)
                    val dialog = Dialog().apply {
                        headerTitle = "Завантажити готовий файл"

                        val downloadButton = Button().apply {
                            icon = Icon(VaadinIcon.DOWNLOAD)
                            addThemeVariants(ButtonVariant.LUMO_ICON)
                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                        }
                        val downloadWrapper = FileDownloadWrapper(file)
                        downloadWrapper.wrapComponent(downloadButton)
                        add(NativeLabel(file!!.name), downloadWrapper)
                    }
                    dialog.open()
                }
            }

            val createButton = Button("Створити субтитри").apply {
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                onLeftClick {
                    val createdFiles: MutableMap<String, StreamResource> = mutableMapOf()
                    val actors: MutableList<com.magnariuk.lemonkasubstoolw.data.Classes.Actor> = mutableListOf()
                    api.getProjectAssignments(currentProject!!.id).forEach { assignment ->
                        val actor = api.getActor(assignment.actor)
                        val characters = api.getCharactersByActor(actor.id, currentProject!!.id)
                        actors.add(com.magnariuk.lemonkasubstoolw.data.Classes.Actor(actor.actorName, characters.map { it.name }.toMutableList()))
                    }
                    val assParser = ParserIS()

                    if(selectFormat.value != null){
                        when (selectFormat.value.format){
                            "srt" -> {
                                actors.forEach { actor ->
                                    val trier = assParser.createSubRip("${actor.actorName}.srt", ass!!, actor.characterNames, api.getSeparators().map { it.separator })
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
                                actors.forEach { actor ->
                                    val trier = assParser.createAss("${actor.actorName}.ass", ass!!, actor.characterNames, api.getSeparators().map { it.separator })
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
                    val actors: MutableList<com.magnariuk.lemonkasubstoolw.data.Classes.Actor> = mutableListOf()
                    api.getProjectAssignments(currentProject!!.id).forEach { assignment ->
                        val actor = api.getActor(assignment.actor)
                        val characters = api.getCharactersByActor(actor.id, currentProject!!.id)
                        actors.add(com.magnariuk.lemonkasubstoolw.data.Classes.Actor(actor.actorName, characters.map { it.name }.toMutableList()))
                    }
                    val assParser = ParserIS()

                    if(selectFormat.value != null){
                        when (selectFormat.value.format){
                            "srt" -> {
                                actors.forEach { actor ->
                                    val trier = assParser.createSubRip("${actor}.srt", ass!!, actor.characterNames, api.getSeparators().map { it.separator })
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
                                actors.forEach { actor ->
                                    val trier = assParser.createAss("${actor}.ass", ass!!, actor.characterNames, api.getSeparators().map { it.separator })
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
                VerticalLayout(
                    HorizontalLayout(selectFormat,createButton, separateButton).apply {
                        alignItems = Alignment.CENTER
                        justifyContentMode = JustifyContentMode.CENTER
                    },
                    HorizontalLayout(renameButton).apply {
                        alignItems = Alignment.CENTER
                        justifyContentMode = JustifyContentMode.CENTER
                    }
                ).apply {
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