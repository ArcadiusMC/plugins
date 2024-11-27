# Class Loader Tools
Provides a simple class that's intended to be shaded into a plugin
that allows for easy loading of maven repositories and dependencies
from a `runtime_libraries.json` file in the plugin's resources.
  
Reasoning for this is: It's easier to use the `processResources`
task to replace placeholders in a `runtime_libraries.json` file
than to hardcode and then keep track of dependencies in the plugin
bootstrapper.