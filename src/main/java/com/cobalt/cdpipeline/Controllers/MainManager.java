package com.cobalt.cdpipeline.Controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.project.Project;
import com.atlassian.bamboo.project.ProjectManager;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummaryManager;
import com.cobalt.cdpipeline.cdresult.CDResult;
import com.cobalt.cdpipeline.cdresult.CDResultFactory;

/**
 * The main controller of CDPipeline Plugin Project that handles getting the
 * results needed for displaying the table.
 */
public class MainManager {
	private ProjectManager projectManager;
	private PlanManager planManager;
	private ResultsSummaryManager resultsSummaryManager;
	
	/**
	 * Constructs a MainManager object.
	 * 
	 * @param projectManager The ProjectManager (within Bamboo) to get information about projects.
	 * @param planManager The PlanMananger (within Bamboo) to get information about plans.
	 * @param resultsSummaryManager The ResultsSummaryManager (within Bamboo) to get information
	 *                              about builds.
	 */
	public MainManager(ProjectManager projectManager, PlanManager planManager, 
			                       ResultsSummaryManager resultsSummaryManager) {
        if (projectManager == null || planManager == null || resultsSummaryManager == null) {
            throw new IllegalArgumentException("Null arguments not allowed");
        }
		this.projectManager = projectManager;
		this.planManager = planManager;
		this.resultsSummaryManager = resultsSummaryManager;
	}
	
	/**
	 * Get all the results needed for displaying the CDPipeline table.
	 * 
	 * @return a list of CDResults, where each CDResult represents one row.
	 *         See CDResults for more details.
	 */
	public List<CDResult> getCDResults() {
		List<CDResult> resultList = new ArrayList<CDResult>();
		
		Set<Project> projects = projectManager.getAllProjects();
		for (Project project : projects) {
			String projectName = project.getName();
			String projectKey = project.getKey();
			
			List<TopLevelPlan> plans = planManager.getAllPlansByProject(project, TopLevelPlan.class);
			for (Plan plan : plans) {
				String planName = plan.getName();
				String planKey = plan.getKey();
				List<ResultsSummary> buildList = resultsSummaryManager.getResultSummariesForPlan(plan, 0, 0);
				
				CDResult result = CDResultFactory.createCDResult(projectName, planName, projectKey, planKey, buildList);
				resultList.add(result);
			}
		}
		
		return resultList;
	}
}
