package io.jenkins.plugins.gll;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;


public class JsonCombineBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    private boolean useFrench;

    private String jsonTargetFilePath;
    private String jsonSourceString;

    @DataBoundConstructor
    public JsonCombineBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isUseFrench() {
        return useFrench;
    }
    @DataBoundSetter
    public void setJsonTargetFilePath(String jsonTargetFilePath) {
        this.jsonTargetFilePath = jsonTargetFilePath;
    }

    public String getJsonTargetFilePath() {
        return jsonTargetFilePath;
    }

    public String getJsonSourceString() {
        return jsonSourceString;
    }
    @DataBoundSetter
    public void setJsonSourceString(String jsonSourceString) {
        this.jsonSourceString = jsonSourceString;
    }

    @DataBoundSetter
    public void setUseFrench(boolean useFrench) {
        this.useFrench = useFrench;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        if(jsonSourceString == null)
        {
            listener.getLogger().println("jsonSourceString == null");
            return;
        }
        if(jsonTargetFilePath == null)
        {
            listener.getLogger().println("jsonTargetFilePath == null");
            return;
        }
        Path targetFilePath = Paths.get(workspace.getRemote(),jsonTargetFilePath);
        if(!Files.exists(targetFilePath))
        {
            if(!Files.exists(targetFilePath.getParent()))
                Files.createDirectory(targetFilePath.getParent());

            listener.getLogger().println("File Not Exist:" +targetFilePath);
            Files.write(targetFilePath,jsonSourceString.getBytes(StandardCharsets.UTF_8));
            return;
        }
        listener.getLogger().println("Read From:" +targetFilePath);
        JsonObject targetJson = new JsonParser().parse(new String( Files.readAllBytes(targetFilePath), StandardCharsets.UTF_8 )).getAsJsonObject();
        JsonObject sourceJson =new JsonParser().parse(jsonSourceString).getAsJsonObject();
        OverrideJson(sourceJson, targetJson);
        String targetJsonString = new Gson().toJson(targetJson);
        listener.getLogger().println("CombinedJson Is:" +targetJsonString);
        Files.write(targetFilePath,targetJsonString.getBytes(StandardCharsets.UTF_8));
        listener.getLogger().println("Write To:" +targetFilePath);
    }

    private void OverrideJson(JsonElement sourceObject, JsonElement targetObject)
    {
        if(!(sourceObject instanceof  JsonObject))
            return;
        if(!(targetObject instanceof  JsonObject))
            return;

        JsonObject srcJsonObject = (JsonObject) sourceObject;
        JsonObject tgtJsonObject = (JsonObject) targetObject;

        for(Map.Entry<String, JsonElement> entry : srcJsonObject.entrySet())
        {
            if(entry.getValue() instanceof  JsonObject)
            {
                if( tgtJsonObject.has(entry.getKey()) )
                {
                    OverrideJson(entry.getValue(), tgtJsonObject.get(entry.getKey()));
                }
                else
                {
                    tgtJsonObject.add(entry.getKey(),entry.getValue());
                }
            }
            else
            {
                if( tgtJsonObject.has(entry.getKey()) )
                {
                    tgtJsonObject.remove(entry.getKey());
                }
                tgtJsonObject.add(entry.getKey(),entry.getValue());
            }
        }
    }

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public FormValidation doCheckName(@QueryParameter String value, @QueryParameter boolean useFrench)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error(Messages.HelloWorldBuilder_DescriptorImpl_errors_missingName());
            if (value.length() < 4)
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_tooShort());
            if (!useFrench && value.matches(".*[éáàç].*")) {
                return FormValidation.warning(Messages.HelloWorldBuilder_DescriptorImpl_warnings_reallyFrench());
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }

    }

}
