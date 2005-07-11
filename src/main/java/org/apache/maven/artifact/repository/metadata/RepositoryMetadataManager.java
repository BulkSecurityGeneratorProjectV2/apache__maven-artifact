package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;

public interface RepositoryMetadataManager
{

    void resolve( RepositoryMetadata repositoryMetadata, ArtifactRepository remote, ArtifactRepository local, String remoteRepositoryId )
        throws RepositoryMetadataManagementException;
    
    void deploy( RepositoryMetadata repositoryMetadata, ArtifactRepository remote )
        throws RepositoryMetadataManagementException;
    
    void install( RepositoryMetadata repositoryMetadata, ArtifactRepository local, String remoteRepositoryId )
        throws RepositoryMetadataManagementException;

}