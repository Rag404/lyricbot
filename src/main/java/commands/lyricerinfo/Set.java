package commands.lyricerinfo;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import dataobjects.Lyricer;
import main.LoggerManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import repo.LyricerRepo;
import utils.Utils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Set extends SlashCommand
{
    private static final String CHANNEL_LINK = "channel-link";
    private static final String WATERMARK = "watermark";

    public Set()
    {
        this.name = "set";
        this.help = "Set your own link and/or watermark.";

        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, CHANNEL_LINK, "Link to your own YouTube channel or social media.", false));
        options.add(new OptionData(OptionType.ATTACHMENT, WATERMARK, "Image file of your own watermark, 2mb max file size, PNG only.", false));

        this.options = options;
    }

    @Override
    protected void execute(SlashCommandEvent event)
    {
        event.deferReply().queue();

        if(event.optString(CHANNEL_LINK) == null && event.optAttachment(WATERMARK) == null)
        {
            event.getHook().sendMessage("❌ | You did not provide a link **OR** a watermark.").queue();
            return;
        }

        if(event.optString(CHANNEL_LINK) != null)
        {
            if(!Utils.isValidUrl(event.optString(CHANNEL_LINK)))
                event.getHook().sendMessage("❌ | The link you provided isn't valid!").queue();
            else
                event.getHook().sendMessage(setLink(event.getUser().getIdLong(), event.optString(CHANNEL_LINK))).queue();
        }

        if(event.optAttachment(WATERMARK) != null)
        {
            Message.Attachment attachment = event.optAttachment(WATERMARK);

            if(attachment.getSize() > 2097152)
                event.getHook().sendMessage("❌ | Your image is over 2mb. Please try again.").queue();
            else if(!attachment.getContentType().equalsIgnoreCase("image/png"))
                event.getHook().sendMessageFormat("❌ | Only PNGs are accepted. Please try again. (expected: `image/png`, received: `%s`)", attachment.getContentType()).queue();
            else
                setImage(event.getHook(), event.getUser().getId(), event.optAttachment(WATERMARK));
        }
    }

    private String setLink(long userId, String link)
    {
        try
        {
            Lyricer lyricer = new Lyricer().setUserId(userId).setLink(link);

            if(!LyricerRepo.doesExist(userId))
            {
                LyricerRepo.addLyricer(lyricer);
                return "✅ | Your link has been saved.\n";
            }

            else
            {
                LyricerRepo.updateLink(userId, link);
                return "✅ | Your link has been updated.\n";
            }
        }

        catch(SQLException e)
        {
            LoggerManager.logError(e, "setting link");
            return "❌ | There was an error saving your link.";
        }
    }

    private void setImage(InteractionHook hook, String userId, Message.Attachment watermark)
    {
        String fileName = "watermarks/" + userId + ".png";
        boolean exists = new File(fileName).exists();

        watermark.getProxy().downloadToFile(new File(fileName))
                 .thenAccept(file -> {
                     if(exists)
                         hook.sendMessage("✅ | Your watermark has been updated.").queue();
                     else
                         hook.sendMessage("✅ | Your watermark has been saved.").queue();
                 })
                 .exceptionally(t -> {
                     LoggerManager.logError(new Exception(t), "setting watermark");
                     hook.sendMessage("❌ | There was an error saving your watermark.").queue();
                     return null;
                 });
    }
}

