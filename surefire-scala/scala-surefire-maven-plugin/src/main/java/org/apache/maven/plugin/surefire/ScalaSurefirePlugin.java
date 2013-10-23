package org.apache.maven.plugin.surefire;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.util.NestedRuntimeException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David Pratt (dpratt@vast.com)
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ScalaSurefirePlugin extends SurefirePlugin {

    private static final String SCALA_GROUPID = "org.scala-lang";
    private static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";

    private VersionNumber _scalaVersionN;

    /**
     * Allows you to specify the name of the ScalaTest artifact. If not set, <code>org.testng:testng</code> will be used.
     * Note - the appropriate major/minor version combination for the detected version of Scala will be appended to this
     * for dependency resolution purposes.
     */
    @Parameter(property = "scalaTestArtifactName", defaultValue = "org.scalatest:scalatest")
    protected String scalaTestArtifactName;

    @Parameter(property = "scala.version")
    private String scalaVersion;

    @Override
    protected List<ProviderInfo> createProviders()
            throws MojoFailureException, MojoExecutionException {
        final Artifact junitDepArtifact = getJunitDepArtifact();
        ProviderList wellKnownProviders =
                new ProviderList(new DynamicProviderInfo(null),
                        new ScalaTestProviderInfo(getScalaTestArtifact()),
                        new TestNgProviderInfo(getTestNgArtifact()),
                        new JUnitCoreProviderInfo(getJunitArtifact(), junitDepArtifact),
                        new JUnit4ProviderInfo(getJunitArtifact(), junitDepArtifact),
                        new JUnit3ProviderInfo());

        return wellKnownProviders.resolve(getLog());
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        super.execute();
    }

    protected Artifact getScalaTestArtifact() throws MojoFailureException {
        StringBuilder artifactName = new StringBuilder(scalaTestArtifactName);
        artifactName.append("_");
        VersionNumber scalaV = findScalaVersion();
        artifactName.append(scalaV.major)
                .append('.')
                .append(scalaV.minor);
        if (scalaV.major == 2 && scalaV.minor <= 9) {
            artifactName.append('.').append(scalaV.bugfix);
        }
        getLog().debug("Using ScalaTest artifact name " + artifactName.toString());
        return getProjectArtifactMap().get(artifactName.toString());
    }

    //copied from the superclass as well - sadly, private as well
    protected Artifact getJunitArtifact() {
        return getProjectArtifactMap().get(getJunitArtifactName());
    }

    protected Artifact getJunitDepArtifact() {
        return getProjectArtifactMap().get("junit:junit-dep");
    }

    protected Artifact getTestNgArtifact()
            throws MojoExecutionException {
        Artifact artifact = getProjectArtifactMap().get(getTestNGArtifactName());

        if (artifact != null) {
            VersionRange range = createVersionRange();
            if (!range.containsVersion(new DefaultArtifactVersion(artifact.getVersion()))) {
                throw new MojoExecutionException(
                        "TestNG support requires version 4.7 or above. You have declared version "
                                + artifact.getVersion());
            }
        }
        return artifact;
    }

    protected VersionRange createVersionRange() {
        try {
            return VersionRange.createFromVersionSpec("[4.7,)");
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }


    class ScalaTestProviderInfo implements ProviderInfo {
        private final Artifact scalaTestArtifact;

        ScalaTestProviderInfo(Artifact scalaTestArtifact) {
            this.scalaTestArtifact = scalaTestArtifact;
        }

        public String getProviderName() {
            return "com.vast.surefire.scalatest.ScalaTestProvider";
        }

        public boolean isApplicable() {
            return scalaTestArtifact != null;
        }

        public void addProviderProperties() {
        }

        public Classpath getProviderClasspath()
                throws ArtifactResolutionException, ArtifactNotFoundException {
            Artifact thisArtifact = getPluginArtifactMap().get("com.vast:scala-surefire-maven-plugin");
            return resolveProviderClasspath("surefire-scalatest", thisArtifact.getBaseVersion(),
                    scalaTestArtifact);
        }
    }

    protected Classpath resolveProviderClasspath(String provider, String version, Artifact filteredArtifact )
            throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Classpath classPath = ClasspathCache.getCachedClassPath( provider );
        if ( classPath == null )
        {
            Artifact providerArtifact = artifactFactory.createDependencyArtifact( "com.vast", provider,
                    VersionRange.createFromVersion(
                            version ), "jar", null,
                    Artifact.SCOPE_TEST );
            ArtifactResolutionResult result = resolveArtifact( filteredArtifact, providerArtifact );
            List<String> files = new ArrayList<String>();

            for ( Object o : result.getArtifacts() )
            {
                Artifact artifact = (Artifact) o;

                getLog().debug("Adding to scala-surefire test classpath: " + artifact.getFile().getAbsolutePath() + " Scope: "
                        + artifact.getScope());

                files.add( artifact.getFile().getAbsolutePath() );
            }
            classPath = new Classpath( files );
            ClasspathCache.setCachedClasspath( provider, classPath );
        }
        return classPath;
    }

    protected ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter = new ExcludesArtifactFilter(
                    Collections.singletonList(filteredArtifact.getGroupId() + ":" + filteredArtifact.getArtifactId()) );
        }

        Artifact originatingArtifact = getArtifactFactory().createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        try
        {
            return getArtifactResolver().resolveTransitively( Collections.singleton( providerArtifact ),
                    originatingArtifact, getLocalRepository(),
                    getRemoteRepositories(), getMetadataSource(), filter );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new NestedRuntimeException( e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new NestedRuntimeException( e );
        }
    }


    protected VersionNumber findScalaVersion() throws MojoFailureException {
        if (_scalaVersionN == null) {
            String detectedScalaVersion = scalaVersion;
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                detectedScalaVersion = findScalaVersionFromDependencies();
            }
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                if (!"pom".equals(project.getPackaging().toLowerCase())) {
                    getLog().warn("you don't define " + SCALA_GROUPID + ":" + SCALA_LIBRARY_ARTIFACTID + " as a dependency of the project");
                }
                detectedScalaVersion = "0.0.0";
            } else {
                // grappy hack to retrieve the SNAPSHOT version without timestamp,...
                // because if version is -SNAPSHOT and artifact is deploy with uniqueValue then the version
                // get from dependency is with the timestamp and a build number (the resolved version)
                // but scala-compiler with the same version could have different resolved version (timestamp,...)
                boolean isSnapshot = ArtifactUtils.isSnapshot(detectedScalaVersion);
                if (isSnapshot && !detectedScalaVersion.endsWith("-SNAPSHOT")) {
                    detectedScalaVersion = detectedScalaVersion.substring(0, detectedScalaVersion.lastIndexOf('-', detectedScalaVersion.lastIndexOf('-') - 1)) + "-SNAPSHOT";
                }
            }
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                throw new MojoFailureException("no scalaVersion detected or set");
            }
            if (StringUtils.isNotEmpty(scalaVersion)) {
                if (!scalaVersion.equals(detectedScalaVersion)) {
                    getLog().warn("scala library version define in dependencies doesn't match the scalaVersion of the plugin");
                }
                //getLog().info("suggestion: remove the scalaVersion from pom.xml"); //scalaVersion could be define in a parent pom where lib is not required
            }
            _scalaVersionN = new VersionNumber(detectedScalaVersion);
        }
        return _scalaVersionN;
    }

    private String findScalaVersionFromDependencies() {
        return findVersionFromDependencies(SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID);
    }

    //TODO refactor to do only one scan of dependencies to find version
    protected String findVersionFromDependencies(String groupId, String artifactId) {
        String version = null;
        for (Dependency dep : getDependencies()) {
            if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                version = dep.getVersion();
            }
        }
        if (StringUtils.isEmpty(version)) {
            List<Dependency> deps = new ArrayList<Dependency>();
            deps.addAll(project.getModel().getDependencies());
            if (project.getModel().getDependencyManagement() != null) {
                deps.addAll(project.getModel().getDependencyManagement().getDependencies());
            }
            for (Dependency dep : deps) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    version = dep.getVersion();
                }
            }
        }
        return version;
    }

    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

}
