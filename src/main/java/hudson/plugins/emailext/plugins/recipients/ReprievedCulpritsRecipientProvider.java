
package hudson.plugins.emailext.plugins.recipients;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import hudson.plugins.emailext.plugins.RecipientProvider;
import hudson.plugins.emailext.plugins.RecipientProviderDescriptor;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.mail.internet.InternetAddress;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Recipient Provider to inform culprits that the build has been fixed.
 */
public class ReprievedCulpritsRecipientProvider extends RecipientProvider {
    
    @DataBoundConstructor
    public ReprievedCulpritsRecipientProvider() {
        
    }
    
    @Override
    public void addRecipients(final ExtendedEmailPublisherContext context, EnvVars env, Set<InternetAddress> to, Set<InternetAddress> cc, Set<InternetAddress> bcc) {
        final class Debug implements RecipientProviderUtilities.IDebug {
            private final ExtendedEmailPublisherDescriptor descriptor
                    = Jenkins.getActiveInstance().getDescriptorByType(ExtendedEmailPublisherDescriptor.class);

            private final PrintStream logger = context.getListener().getLogger();

            public void send(final String format, final Object... args) {
                descriptor.debug(logger, format, args);
            }
        }
        final Debug debug = new Debug();
        Run<?,?> run = context.getRun();
        final Result runResult = run.getResult();
        
        if (runResult.isBetterOrEqualTo(Result.SUCCESS)) {
        	Run<?,?> previousRun = run.getPreviousCompletedBuild();
        	if (previousRun != null) {
        		final Result previousRunResult = previousRun.getResult();
        		if (previousRunResult.isWorseThan(Result.SUCCESS)) {
        	        if (previousRun instanceof AbstractBuild) {
        	            Set<User> users = ((AbstractBuild<?,?>)previousRun).getCulprits();
        	            RecipientProviderUtilities.addUsers(users, context.getListener(), env, to, cc, bcc, debug);
        	        } else  {
        	            List<Run<?, ?>> builds = new ArrayList<>();
        	            Run<?, ?> build = previousRun;
        	            while (build != null) {
        	                final Result buildResult = build.getResult();
        	                if (buildResult != null) {
        	                    if (buildResult.isWorseThan(Result.SUCCESS)) {
        	                        debug.send("Including build %s with status %s", build.getId(), buildResult);
        	                        builds.add(build);
        	                    } else {
        	                        break;
        	                    }
        	                }
        	                build = build.getPreviousCompletedBuild();
        	            }
        	            Set<User> users = RecipientProviderUtilities.getChangeSetAuthors(builds, debug);
        	            RecipientProviderUtilities.addUsers(users, context.getListener(), env, to, cc, bcc, debug);
        	        }        			
        		}
        	}
        }
    }

    @Extension
    public static final class DescriptorImpl extends RecipientProviderDescriptor {
        
        @Override
        public String getDisplayName() {
            return "Reprieved Culprits";
        }
        
    }
    
}
