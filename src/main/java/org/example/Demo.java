package org.example;

import com.google.common.collect.Lists;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use the aether api to download jar packages from the specified maven Repository
 *
 * @author wxb
 */
public class Demo {

    private static final String DOWNLOAD_PATH = "/usr/local/test/mvnrepo";

    public static void main(String[] args) throws Exception {
        List<Artifact> artifacts = Lists.newArrayList(
                new DefaultArtifact("org.eclipse.aether", "aether-transport-http", "jar", "1.1.0"));
        resolveArtifacts(artifacts);
    }

    private static void resolveArtifacts(List<Artifact> artifacts) throws Exception {
        RepositorySystem repoSystem = buildRepositorySystem();
        RepositorySystemSession session = newSessopm(repoSystem, DOWNLOAD_PATH);
        List<RemoteRepository> remoteRepos = getRemoteRepos();

        // read relevant artifact descriptor info
        List<Artifact> resolvedArtifacts = artifacts.stream()
                .map(artifact -> new ArtifactDescriptorRequest(artifact, remoteRepos, null))
                .map(artDescReq -> {
                    try {
                        return repoSystem.readArtifactDescriptor(session, artDescReq);
                    } catch (ArtifactDescriptorException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(ar -> ar.getDependencies().stream())
                .filter(dependency -> "compile".equals(dependency.getScope()))
                .map(Dependency::getArtifact)
                .collect(Collectors.toList());

       artifacts.addAll(resolvedArtifacts);

        //download jar
        List<ArtifactRequest> artReqs = resolvedArtifacts.stream().map(artifact -> new ArtifactRequest(artifact, remoteRepos, null)).collect(Collectors.toList());
        Set<File> files = repoSystem.resolveArtifacts(session, artReqs).stream().map(artifactResult -> artifactResult.getArtifact().getFile()).collect(Collectors.toSet());
        for (File file : files) {
            System.out.println(file);
        }
    }

    /**
     * create a repository system session
     */
    private static RepositorySystemSession newSessopm(RepositorySystem repoSystem, String target) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, new LocalRepository(target)));
        return session;
    }

    /**
     * Build the RepositorySystem, which is the main interface used to operate the Maven Repository
     */
    private static RepositorySystem buildRepositorySystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    /**
     * get remote Repository
     */
    private static List<RemoteRepository> getRemoteRepos() {
        List<RemoteRepository> reps = new ArrayList<>();
        RemoteRepository.Builder builder = new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/");
        reps.add(builder.build());
        return reps;
    }

}