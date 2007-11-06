package org.apache.maven.artifact;

public enum ArtifactScopeEnum 
{
      compile(1)
    , test(2)
    , runtime(3)
    , provided(4)
    , system(5)
    ;

      public static final ArtifactScopeEnum DEFAULT_SCOPE = compile;
      private int id;

	// Constructor 
	ArtifactScopeEnum( int id )
	{
	  this.id = id;
	}

	int getId()
	{
	  return id;
	}
}