/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.groupon.jenkins.dynamic.build.execution;

import com.google.common.collect.Iterables;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.DynamicSubProject;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import hudson.model.BuildListener;
import hudson.model.Result;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DynamicMultiConfigBuildRunner implements DynamicBuildRunner {
	private static final Logger LOGGER = Logger.getLogger(DynamicMultiConfigBuildRunner.class.getName());
	private final DynamicBuild build;
	private final SubBuildScheduler subBuildScheduler;
	private final DotCiPluginRunner dotCiPluginRunner;

	public DynamicMultiConfigBuildRunner(DynamicBuild build, BuildType buildType, DotCiPluginRunner dotCiPluginRunner) {
		this(build, new SubBuildScheduler(build, new SubBuildParamsAction(build.getBuildConfiguration(), buildType)), dotCiPluginRunner, buildType);
	}

	public DynamicMultiConfigBuildRunner(DynamicBuild build, SubBuildScheduler subBuildScheduler, DotCiPluginRunner dotCiPluginRunner, BuildType buildType) {
		this.build = build;
		this.subBuildScheduler = subBuildScheduler;
		this.dotCiPluginRunner = dotCiPluginRunner;
	}

	@Override
	public Result runBuild(BuildListener listener) throws InterruptedException, IOException {
		try {
			Result combinedResult = subBuildScheduler.runSubBuilds(build.getRunSubProjects(), listener);
			Iterable<DynamicSubProject> postBuildSubProjects = build.getPostBuildSubProjects();
			if (combinedResult.equals(Result.SUCCESS) && !Iterables.isEmpty(postBuildSubProjects)) {
				Result runSubBuildResults = subBuildScheduler.runSubBuilds(postBuildSubProjects, listener);
				combinedResult = combinedResult.combine(runSubBuildResults);
			}
			build.setResult(combinedResult);
			dotCiPluginRunner.runPlugins(listener);
			return combinedResult;
		} finally {
			try {
				subBuildScheduler.cancelSubBuilds(listener.getLogger());
			} catch (Exception e) {
				// There is nothing much we can do at this point
				LOGGER.log(Level.SEVERE, "Failed to cancel subbuilds", e);
			}
		}
	}
}
