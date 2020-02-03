package mindustry.plugin;

import mindustry.Vars;
import mindustry.entities.type.Player;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.permission.Role;
import org.json.JSONObject;

import mindustry.plugin.discordcommands.DiscordCommands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class BotThread extends Thread {
    public DiscordApi api;
    private Thread mt;
    private JSONObject data;
    public DiscordCommands commandHandler = new DiscordCommands();

    public BotThread(DiscordApi api, Thread mt, JSONObject data) {
        this.api = api; //new DiscordApiBuilder().setToken(data.get(0)).login().join();
        this.mt = mt;
        this.data = data;

        // register commands
        this.api.addMessageCreateListener(commandHandler);
        new ComCommands().registerCommands(commandHandler);
        new ServerCommands(data).registerCommands(commandHandler);
        new MessageCreatedListeners(data).registerListeners(commandHandler);
    }

    public void run(){
        while (this.mt.isAlive()){
            try {
                Thread.sleep(60 * 1000);

                // increment playtime for users in-game
                for (Player p : Vars.playerGroup.all()) {
                    if(IoPlugin.database.containsKey(p.uuid)) {
                        IoPlugin.database.get(p.uuid).incrementPlaytime(1); // 1 minute
                    } else {
                        IoPlugin.database.put(p.uuid, new PlayerData(0));
                    }
                }

                // save database

                try {
                    File fileOne =new File("database.io");
                    FileOutputStream fos=new FileOutputStream(fileOne);
                    ObjectOutputStream oos=new ObjectOutputStream(fos);

                    oos.writeObject(IoPlugin.database);
                    oos.flush();
                    oos.close();
                    fos.close();

                } catch(Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {}
        }
        if (data.has("serverdown_role_id")){
            Role r = new UtilMethods().getRole(api, data.getString("serverdown_role_id"));
            TextChannel tc = new UtilMethods().getTextChannel(api, data.getString("serverdown_channel_id"));
            if (r == null || tc ==  null) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            } else {
                if (data.has("server_name")){
                    String serverName = data.getString("server_name");
                    new MessageBuilder()
                            .append(String.format("%s\nServer %s is down",r.getMentionTag(),((serverName != "") ? ("**"+serverName+"**") : "")))
                            .send(tc);
                } else {
                    new MessageBuilder()
                            .append(String.format("%s\nServer is down.", r.getMentionTag()))
                            .send(tc);
                }
            }
        }
        api.disconnect();
    }
}
