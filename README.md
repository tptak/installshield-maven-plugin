maven-installshield-plugin
==========================

A plugin to handle InstallShield builds from maven

InstallShield by Flexera is a powerful tool to help deliver whatever your ding to wherever it is supposed to run. It provides a lot of flexibility which causes, that without proper clear methods larger installer projects may become messy.

The idea behind this plugin is to give some structure to one's installer project. My installer project for starters.

It is intended that two packaging types are made available:
 * prz
     * It is intended to act as a prerequisite.
     * This is a prerequisite package, containing prq file and at least all files referenced in it (currently it's rather everything from DiskImages folder from InstallShield output, everything from static files folder and all referenced files from dependency folder).
     * It is allowed that the prz project doesn't contain an InstallShield project file (use case: a prerequisite containing a third party installer)
 * msz
    * Intended as a final result of an installer build
    * Still needs specification what it should be
    * One option would be to give possibility to deploy it to Nexus as an iso

Notes
=====

There may be stuff missing. I don't say there isn't. This is caused by my level of knowledge about InstallShield projects and by my needs. If you need something else - let's talk.

How to use the plugin
=====================

Have a look at the samples project. The project is poorly documented and I wouldn't expect anything else before I make it run with a more sophisticated installers than the samples. Right no the concept is prone to big changes with loss of compatibility.

Hints
=====

By default it is assumed that ISCmdBld.exe is on the Path if not, you need to set installshieldExecutable.

If you want to make a prz project without the InstallShield, you need to set failWhenNoInstallshieldFile to false.
