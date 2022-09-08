import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.requests.GatewayIntent


fun main(args: Array<String>) {
    val jda = JDABuilder.create(
        args[0],
        GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)
    )/*.setMemberCachePolicy(MemberCachePolicy.ALL).setChunkingFilter(ChunkingFilter.ALL)
        .enableCache(CacheFlag.ONLINE_STATUS)*/.build()

    jda.presence.setStatus(OnlineStatus.ONLINE)
    jda.addEventListener(NewMemberJoin(), IsBotAlive(), /*CommandManager(),*/ RegistrationBot(), SubjectChannelManager())
}

class IsBotAlive : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        val msg = event.message
        if (msg.contentRaw == "!ping") {
            val channel: MessageChannel = event.channel
            val time = System.currentTimeMillis()
            channel.sendMessage("!ping") /* => RestAction<Message> */
                .queue { response: Message ->
                    response.editMessageFormat(
                        "Pong: %d ms",
                        System.currentTimeMillis() - time
                    ).queue()
                }
        }
    }
}

class NewMemberJoin : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.addRoleToMember(
            event.member, event.guild.getRolesByName("Регистрация", true)
                .first()
        ).complete()
    }
}


class CommandManager : ListenerAdapter() {

    /**
     * Listens for slash commands and responds accordingly
     */

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        val command = event.name
        if (command == "welcome") {
            // Run the 'ping' command
            val userTag = event.user.asTag
            event.reply("Welcome to the server, **$userTag**!").queue()
        } else if (command == "roles") {
            // run the 'roles' command
            event.deferReply().queue()
            var response = ""
            for (role in event.guild!!.roles) {
                response += """
                ${role.asMention}
                
                """.trimIndent()
            }
            event.hook.sendMessage(response).queue()
        }
    }


}

//Очистка чата
fun clearChannel(channel: TextChannel){
    var deletingFlag = true
    val history = MessageHistory(channel)
    while (deletingFlag) {
        val messages = history.retrievePast(100).complete()
        if (messages.size > 1)
            channel.deleteMessages(messages).queue()
        else
            deletingFlag = false
    }
}

class RegistrationBot : ListenerAdapter() {

    private val courseRoles: MutableList<Role> = mutableListOf()
    private lateinit var registrationRole: Role
    private lateinit var professorRole: Role

    override fun onGuildReady(event: GuildReadyEvent) {

        registrationRole = event.guild.getRolesByName("Регистрация", true).first()
        professorRole = event.guild.getRolesByName("Преподаватель", true).first()
        for (i in 1..4)
            courseRoles.add(event.guild.getRolesByName("СП $i", true).first())

        val channel = event.guild.getCategoriesByName("Регистрация", true)
            .first().textChannels.find { it.name == "регистрация" } ?: return //логгер

       clearChannel(channel)

        /*val commandData: MutableList<CommandData> = ArrayList()
        commandData.add(Commands.slash("welcome", "Get welcomed by the bot"))
        commandData.add(Commands.slash("roles", "Display all roles on the server"))
        event.guild.updateCommands().addCommands(commandData).queue()*/
        channel.sendMessage("Рады приветствовать вас на официальном сервере программы Современное Программирование!")
            .complete()
        channel.sendMessage("Вы:").setActionRow(sendStudentAndProfessor()).queue()

        /* val member = event.guild.members.find{it.nickname == "Mellorr"}
         println(member?.nickname)
         println(member?.idLong!!)
         println((event.guild.getCategoriesByName("СП 2", true).first().textChannels.find { it.name == "болталка" })?.name)
         (event.guild.getCategoriesByName("СП 2", true).first().textChannels.find { it.name == "болталка" })!!.manager.putMemberPermissionOverride(member?.idLong!!, mutableListOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null).queue()
 */
    }

    private fun sendStudentAndProfessor(): List<Button> {
        val buttons: MutableList<Button> = mutableListOf()
        buttons.add(Button.primary("student", "Студент"))
        buttons.add(Button.primary("professor", "Преподаватель"))
        return buttons
    }

    /*override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "hello") {
            event.reply("Click the button to say hello")
                .addActionRow(
                    Button.primary("hello", "Click Me"),  // Button with only a label
                    Button.success("emoji", Emoji.fromFormatted("<:minn:245267426227388416>"))) // Button with only an emoji
                .queue()
        } else if (event.name == "info") {
            event.reply("Click the buttons for more info")
                .addActionRow( // link buttons don't send events, they just open a link in the browser when clicked
                    Button.link("https://github.com/DV8FromTheWorld/JDA", "GitHub")
                        .withEmoji(Emoji.fromFormatted("<:github:849286315580719104>")),  // Link Button with label and emoji
                    Button.link("https://ci.dv8tion.net/job/JDA/javadoc/", "Javadocs")) // Link Button with only a label
                .queue()
        }
    }*/


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val member = event.member ?: return

        val name = TextInput.create("name", "Name", TextInputStyle.SHORT)
            .setPlaceholder("Name")
            .setRequiredRange(1, 50)
            .setPlaceholder("Иван")
            .build()

        val surname = TextInput.create("surname", "Surname", TextInputStyle.SHORT)
            .setPlaceholder("Surname")
            .setRequiredRange(1, 50)
            .setPlaceholder("Иванов")
            .build()

        when (event.componentId) {
            "student" -> {
                if (member.roles.contains(professorRole))
                    return
                val courseNumber = TextInput.create("courseNumber", "courseNumber", TextInputStyle.SHORT)
                    .setPlaceholder("courseNumber")
                    .setRequiredRange(1, 1)
                    .setPlaceholder("1")
                    .build()

                val studentRegModal = Modal.create("student profile", "Setting student profile")
                    .addActionRows(ActionRow.of(surname), ActionRow.of(name), ActionRow.of(courseNumber))
                    .build()

                event.replyModal(studentRegModal).queue()
            }

            "professor" -> {
                if (member.roles.any{courseRoles.contains(it)})
                    return
                val professorRegModal = Modal.create("professor profile", "Setting professor profile")
                    .addActionRows(ActionRow.of(surname), ActionRow.of(name))
                    .build()

                event.replyModal(professorRegModal).queue()
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        val member = event.member ?: return
        val guild = event.guild ?: return

        when (event.modalId){
            "student profile" -> {
                val surname = event.getValue("surname")?.asString ?: "Error" //логгер
                val name = event.getValue("name")?.asString ?: "Error" //логгер
                val course = event.getValue("course")?.asString?.toIntOrNull() //логгер

                if (course == null || course !in 1..4) {
                    event.reply("Hi, you have entered wrong course number.\n " +
                            "It should be a number in range 1..4.\n" +
                            "Try again, please, or contact administration for help.")
                        .setEphemeral(true).queue()
                    return
                }

                val chosenRole = courseRoles[course - 1]

                member.modifyNickname("$surname $name".trim()).queue()
                member.roles.forEach { guild.removeRoleFromMember(member, it) }
                guild.addRoleToMember(member, chosenRole).complete()
                guild.removeRoleFromMember(member, registrationRole).complete()
                event.reply("Hi, $surname $name!\n You have been successfully registered!")
                    .setEphemeral(true).queue()
            }

            "professor profile" -> {
                val surname = event.getValue("surname")?.asString ?: "Error"
                val name = event.getValue("name")?.asString ?: "Error"
                member.modifyNickname("$surname $name".trim()).queue()

                guild.addRoleToMember(member,professorRole).queue()
                guild.removeRoleFromMember(member, registrationRole).queue()
                event.reply("Hello, $surname $name!\n You have been successfully registered!").setEphemeral(true).queue()
            }
        }
    }
}

class SubjectChannelManager: ListenerAdapter() {

    private fun sendCreateAndJoin(): List<Button> {
        val buttons: MutableList<Button> = mutableListOf()
        buttons.add(Button.primary("create", "Создать"))
        buttons.add(Button.primary("join", "Присоединиться"))
        return buttons
    }

    override fun onGuildReady(event: GuildReadyEvent) {
        val channel = event.guild.getCategoriesByName("Управление курсами", true)
            .first().textChannels.find { it.name == "взаимодействие-с-курсами" } ?: return //логгер

        clearChannel(channel)

        channel.sendMessage("Этот чат предназначен для создания каналов для курсов и присоединения к уже существующим курсам.").complete()
        channel.sendMessage("Ознакомиться с полным списком курсов Вы можете в соседнем канале.").complete()
        channel.sendMessage("Вы хотите:").setActionRow(sendCreateAndJoin()).queue()


    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {

        val courseNumber = TextInput.create("courseNumber", "Course number", TextInputStyle.SHORT)
            .setPlaceholder("Course number")
            .setRequiredRange(1, 1)
            .setPlaceholder("1")
            .build()

        val courseName = TextInput.create("courseName", "Course name", TextInputStyle.SHORT)
            .setPlaceholder("Course name")
            .setRequiredRange(1, 150)
            .setPlaceholder("Теоретическая информатика (практика)")
            .build()

        when (event.componentId) {
            "create" -> {
                val courseCreation = Modal.create("course create", "Course creation modal")
                    .addActionRows(ActionRow.of(courseNumber), ActionRow.of(courseName))
                    .build()

                event.replyModal(courseCreation).queue()
            }
// Если введён не тот курс в reply дописать, что названия курсов можно найти там-то, там-то
            "join" -> {
                val courseJoin = Modal.create("course join", "Course join modal")
                    .addActionRows(ActionRow.of(courseNumber), ActionRow.of(courseName))
                    .build()

                event.replyModal(courseJoin).queue()
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        val member = event.member ?: return
        val guild = event.guild ?: return

        when (event.modalId){
            "course create" -> {
                val courseNumber = event.getValue("courseNumber")?.asString?.toIntOrNull() //логгер
                val courseName = event.getValue("courseName")?.asString ?: "Error" //логгер

                if (courseNumber == null || courseNumber !in 1..4) {
                    event.reply("Hi, you have entered wrong course number.\n " +
                            "It should be a number in range 1..4.\n" +
                            "Try again, please, or contact administration for help.")
                        .setEphemeral(true).queue()
                    return
                }

                val category = guild.getCategoriesByName("СП $courseNumber", true).first()
                category.createTextChannel(courseName).addMemberPermissionOverride(member.idLong, mutableListOf(
                    Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL), null).queue()

                event.reply("Channel $courseName was created successfully!")
                    .setEphemeral(true).queue()
            }

            "course join" -> {
                val courseNumber = event.getValue("courseNumber")?.asString?.toIntOrNull() //логгер
                val courseName = event.getValue("courseName")?.asString ?: "Error" //логгер

                val category = guild.getCategoriesByName("СП $courseNumber", true).first()
                val channel = category.textChannels.find { it.name == courseName }
                if (channel == null)
                {
                    event.reply("Problems with course name.").setEphemeral(true).queue()
                    return
                }
                channel.manager.putMemberPermissionOverride(member.idLong, mutableListOf(
                    Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL), null).queue()

                event.reply("Channel $courseName was updated successfully!\n Check СП $courseNumber category.")
                    .setEphemeral(true).queue()
            }
        }
    }
}


/*
* Тесты: 2 курса с одинаковым именем
* подтверждение роли
* много регистраций одновременно
* Ручное тестирование
*
* Задачи: действия отправлять в queue, а не в comlete
* Разделение по файлам
* Логгирование
* Список актуальных курсов
* Инструкции по взаимодействию с ботом
*
* */
