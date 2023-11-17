package org.shw.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MInvoiceLine;
import org.compiere.model.MProject;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class ProjectCalculation {
	/**
	 * 	Constructor
	 *	@param ctx context
	 *	@param projectID 
	 *	@param trxName trx
	 */

	public ProjectCalculation(Properties ctx, int projectID, String trxName)
	{
		this.projectID = projectID;
		if (projectID > 0)
			project = new MProject(ctx, projectID, trxName);
	}
	//	Project


	/** The ID					*/
	private int projectID = 0;
	/** Project					*/
	MProject project = null;
	
	public String updateProjectPerformanceCalculation() {
		BigDecimal result = Env.ZERO;
		
		// Update Prices
		result = calcLineNetAmt();
		project.set_ValueOfColumn("ProjectPriceListRevenuePlanned", result.setScale(2, RoundingMode.HALF_UP));
		result = calcActualamt();
		project.set_ValueOfColumn("ProjectOfferedRevenuePlanned", result.setScale(2, RoundingMode.HALF_UP));
		
		// Update costs
		// Parameters:                     Project         isSOtrx  isParentProject
		result = calcCostOrRevenuePlanned(projectID, false, false);      // Planned costs from Purchase Orders this project
		project.set_ValueOfColumn("CostPlanned", result.setScale(2, RoundingMode.HALF_UP));
		result = calcCostOrRevenueActual(projectID, false, false); 		 // Actual costs from Purchase Invoices
		project.set_ValueOfColumn("CostAmt", result.setScale(2, RoundingMode.HALF_UP));
		result = calcNotInvoicedCostOrRevenue(projectID, false, false);  // Planned but not yet invoiced costs
		project.set_ValueOfColumn("CostNotInvoiced", result.setScale(2, RoundingMode.HALF_UP));
		BigDecimal costExtrapolated = calcCostOrRevenueExtrapolated(projectID, false, false); // Actual costs + Planned but not yet invoiced costs
		project.set_ValueOfColumn("CostExtrapolated", costExtrapolated.setScale(2, RoundingMode.HALF_UP));

		// Update revenues
		result = calcCostOrRevenuePlanned(projectID, true, false);     // Planned revenue from Sales Orders this project
		project.set_ValueOfColumn("RevenuePlanned", result.setScale(2, RoundingMode.HALF_UP));
		result = calcCostOrRevenueActual(projectID, true, false); 	   // Actual revenue from Sales Invoices
		project.set_ValueOfColumn("RevenueAmt", result.setScale(2, RoundingMode.HALF_UP));
		result = calcNotInvoicedCostOrRevenue(projectID, true, false); // Planned but not yet invoiced revenue
		project.set_ValueOfColumn("RevenueNotInvoiced", result.setScale(2, RoundingMode.HALF_UP));
		BigDecimal revenueExtrapolated = calcCostOrRevenueExtrapolated(projectID, true, false);  // Actual revenue + Planned but not yet invoiced revenue
		project.set_ValueOfColumn("RevenueExtrapolated", revenueExtrapolated.setScale(2, RoundingMode.HALF_UP));
		
		// Update Issue Costs
		BigDecimal costIssueProduct = calcCostIssueProduct(projectID, false);		// Costs of Product Issues
		project.set_ValueOfColumn("CostIssueProduct", costIssueProduct.setScale(2, RoundingMode.HALF_UP));
		BigDecimal costIssueResource = calcCostIssueResource(projectID, false);		// Costs of Resource Issues
		project.set_ValueOfColumn("CostIssueResource", costIssueResource.setScale(2, RoundingMode.HALF_UP));
		BigDecimal costIssueInventory = calcCostIssueInventory(projectID, false);   // Costs of Inventory Issues
		project.set_ValueOfColumn("CostIssueInventory", costIssueInventory.setScale(2, RoundingMode.HALF_UP));
		project.set_ValueOfColumn("CostIssueSum", costIssueProduct.add(costIssueResource).
				add(costIssueInventory).setScale(2, RoundingMode.HALF_UP));  // Issue sum = Costs of Product Issue + Costs of Resource Issue + Costs of Inventory Issues
		project.set_ValueOfColumn("CostDiffExcecution", ((BigDecimal)project.get_Value("CostPlanned")).
				subtract(costIssueProduct).
				subtract(costIssueInventory).setScale(2, RoundingMode.HALF_UP));  // Execution Diff = Planned Costs - (Product Issue Costs + Inventory Issue Costs

		// Gross Margin
		// Gross margin = extrapolated revenue - (extrapolated costs + resource issue costs + inventory issue costs)
		BigDecimal sumCosts = costExtrapolated.add(costIssueResource).add(costIssueInventory);
		
		BigDecimal grossMargin = revenueExtrapolated.subtract(sumCosts);
		project.set_ValueOfColumn("GrossMargin",grossMargin.setScale(2, RoundingMode.HALF_UP));		

		// Margin (%) only for this level; there is no use to calculate it on LL
		if(sumCosts.compareTo(Env.ZERO)==0 && revenueExtrapolated.compareTo(Env.ZERO)==0) {
			project.set_ValueOfColumn("Margin", Env.ZERO); // Costs==0, Revenue== -> 0% margin	
			}
		else if(sumCosts.compareTo(Env.ZERO)!=0) {
			if(revenueExtrapolated.compareTo(Env.ZERO)!=0) {
				project.set_ValueOfColumn("Margin", revenueExtrapolated.divide(sumCosts, 6, RoundingMode.HALF_UP).subtract(Env.ONE).
						multiply(Env.ONEHUNDRED).setScale(2, RoundingMode.HALF_UP));
			}
			else {
				project.set_ValueOfColumn("Margin", Env.ONEHUNDRED.negate()); // Revenue==0 -> -100% margin						
			}

		} 
		else {
			project.set_ValueOfColumn("Margin", Env.ONEHUNDRED); // Costs==0 -> 100% margin			
		}
		
		BigDecimal grossMarginLL = Env.ZERO; // Gross Margin of children

		if (project.isSummary()) { // Project is a parent project
			// Update costs of direct children (not recursively all children!)
			BigDecimal costPlannedLL = calcCostOrRevenuePlannedSons(projectID, false, true);	       // Planned costs from Purchase Orders of children
			project.set_ValueOfColumn("CostPlannedLL", costPlannedLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costAmtLL = calcCostOrRevenueActualSons(projectID, false, true);		           // Actual costs from Purchase Invoices of children
			project.set_ValueOfColumn("CostAmtLL", costAmtLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costNotInvoicedLL = calcNotInvoicedCostOrRevenueSons(projectID, false, true);   // Planned but not yet invoiced costs of children
			project.set_ValueOfColumn("CostNotInvoicedLL", costNotInvoicedLL.setScale(2, RoundingMode.HALF_UP));	
			BigDecimal costExtrapolatedLL = calcCostOrRevenueExtrapolatedSons(projectID, false, true); // Actual costs + Planned but not yet invoiced costs of children
			project.set_ValueOfColumn("CostExtrapolatedLL", costExtrapolatedLL.setScale(2, RoundingMode.HALF_UP));			
			
			// update revenues of children
			BigDecimal revenuePlannedLL = calcCostOrRevenuePlannedSons(projectID, true, true);	         // Planned revenue from Sales Orders of children
			project.set_ValueOfColumn("RevenuePlannedLL", revenuePlannedLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal revenueAmtLL = calcCostOrRevenueActualSons(projectID, true, true);		         // Actual revenue from Sales Invoices of children
			project.set_ValueOfColumn("RevenueAmtLL", revenueAmtLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal revenueNotInvoicedLL = calcNotInvoicedCostOrRevenueSons(projectID, true, true);   // Planned but not yet invoiced revenue of children
			project.set_ValueOfColumn("RevenueNotInvoicedLL", revenueNotInvoicedLL.setScale(2, RoundingMode.HALF_UP));	
			BigDecimal revenueExtrapolatedLL = calcCostOrRevenueExtrapolatedSons(projectID, true, true); // Actual revenue + Planned but not yet invoiced revenue of children
			project.set_ValueOfColumn("RevenueExtrapolatedLL", revenueExtrapolatedLL.setScale(2, RoundingMode.HALF_UP));				
			
			// Update Issue Costs of children
			BigDecimal costIssueProductLL = calcCostIssueProductSons(projectID, true);      // Costs of Product Issues of children
			project.set_ValueOfColumn("CostIssueProductLL", costIssueProductLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costIssueResourceLL = calcCostIssueResourceSons(projectID, true);    // Costs of Resource Issues of children
			project.set_ValueOfColumn("CostIssueResourceLL", costIssueResourceLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costIssueInventoryLL = calcCostIssueInventorySons(projectID, true);  // Costs of Inventory Issues of children
			project.set_ValueOfColumn("CostIssueInventoryLL", costIssueInventoryLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costIssueSumLL  = costIssueProductLL.  // Issue sum LL = Costs of Product Issue LL + Costs of Resource Issue LL+ Costs of Inventory Issue LL
					add(costIssueResourceLL).add(costIssueInventoryLL).setScale(2, RoundingMode.HALF_UP); 
			project.set_ValueOfColumn("CostIssueSumLL", costIssueSumLL.setScale(2, RoundingMode.HALF_UP));
			BigDecimal costDiffExcecutionLL  = costPlannedLL. // Execution Diff LL = Planned Costs LL - (Product Issue Costs LL + Inventory Issue Costs LL)
					subtract(costIssueProductLL).subtract(costIssueInventoryLL).setScale(2, RoundingMode.HALF_UP);
			project.set_ValueOfColumn("CostDiffExcecutionLL", costDiffExcecutionLL.setScale(2, RoundingMode.HALF_UP));

			// Gross margin LL = extrapolated revenue LL - (extrapolated costs LL + resource issue costs LL + inventory issue costs LL)
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).
					subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if(grossMarginLL==null)
				grossMarginLL = Env.ZERO;
			project.set_ValueOfColumn("GrossMarginLL",grossMarginLL.setScale(2, RoundingMode.HALF_UP));

			project.saveEx();  // TODO: delete line

	    	BigDecimal costActualFather       = (BigDecimal)project.get_Value("CostAmt");
	    	BigDecimal costPlannedFather      = (BigDecimal)project.get_Value("CostPlanned");
	    	BigDecimal costExtrapolatedFather = (BigDecimal)project.get_Value("CostExtrapolated");	    	
	    	
	    	// BigDecimal revenuePlannedSons = (BigDecimal)get_Value("RevenuePlannedLL");
	    	BigDecimal revenueExtrapolatedSons =  (BigDecimal)project.get_Value("RevenueExtrapolatedLL");
	    	
	    	BigDecimal weightFather = (BigDecimal)project.get_Value("Weight");
	    	BigDecimal volumeFather = (BigDecimal)project.get_Value("Volume");	    	
	    	
			List<MProject> projectsOfFather = new Query(project.getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", project.get_TrxName())
			.setParameters(projectID)
			.list();
			for (MProject sonProject: projectsOfFather)	{
				//BigDecimal revenuePlannedSon = (BigDecimal)sonProject.get_Value("RevenuePlanned");
				BigDecimal revenueExtrapolatedSon = (BigDecimal)sonProject.get_Value("RevenueExtrapolated");
				BigDecimal weight = (BigDecimal)sonProject.get_Value("Weight");
				BigDecimal volume = (BigDecimal)sonProject.get_Value("volume");
				BigDecimal shareRevenue = Env.ZERO;
				BigDecimal shareWeight = Env.ZERO;
				BigDecimal shareVolume = Env.ZERO;
				if (revenueExtrapolatedSon!=null && revenueExtrapolatedSons!=null && revenueExtrapolatedSons.longValue()!= 0)
					shareRevenue = revenueExtrapolatedSon.divide(revenueExtrapolatedSons, 5, RoundingMode.DOWN);
				if (weight!=null && weightFather != null && weightFather.longValue()!=0)
					shareWeight = weight.divide(weightFather, 5, RoundingMode.DOWN);
				if (volume!=null && volumeFather != null && volumeFather.longValue() != 0)
					shareVolume = volume.divide(volumeFather, 5, RoundingMode.DOWN);
				calcCostPlannedInherited(sonProject, costPlannedFather,costActualFather,costExtrapolatedFather, shareVolume, shareWeight, shareRevenue);
				
				// Collect Low Level Amounts
				costPlannedLL         = costPlannedLL.add((BigDecimal)sonProject.get_Value("CostPlannedLL")==null                ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostPlannedLL"));
				costAmtLL             = costAmtLL.add((BigDecimal)sonProject.get_Value("CostAmtLL")==null                        ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostAmtLL"));
				costNotInvoicedLL     = costNotInvoicedLL.add((BigDecimal)sonProject.get_Value("CostNotInvoicedLL")==null        ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostNotInvoicedLL"));
				costExtrapolatedLL    = costExtrapolatedLL.add((BigDecimal)sonProject.get_Value("CostExtrapolatedLL")==null      ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostExtrapolatedLL"));
				revenuePlannedLL      = revenuePlannedLL.add((BigDecimal)sonProject.get_Value("RevenuePlannedLL")==null          ?Env.ZERO:(BigDecimal)sonProject.get_Value("RevenuePlannedLL"));
				revenueAmtLL          = revenueAmtLL.add((BigDecimal)sonProject.get_Value("RevenueAmtLL")==null                  ?Env.ZERO:(BigDecimal)sonProject.get_Value("RevenueAmtLL"));
				revenueNotInvoicedLL  = revenueNotInvoicedLL.add((BigDecimal)sonProject.get_Value("RevenueNotInvoicedLL")==null  ?Env.ZERO:(BigDecimal)sonProject.get_Value("RevenueNotInvoicedLL"));
				revenueExtrapolatedLL = revenueExtrapolatedLL.add((BigDecimal)sonProject.get_Value("RevenueExtrapolatedLL")==null?Env.ZERO:(BigDecimal)sonProject.get_Value("RevenueExtrapolatedLL"));
				costIssueProductLL    = costIssueProductLL.add((BigDecimal)sonProject.get_Value("CostIssueProductLL")==null      ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostIssueProductLL"));
				costIssueResourceLL   = costIssueResourceLL.add((BigDecimal)sonProject.get_Value("CostIssueResourceLL")==null    ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostIssueResourceLL"));
				costIssueInventoryLL  = costIssueInventoryLL.add((BigDecimal)sonProject.get_Value("CostIssueInventoryLL")==null  ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostIssueInventoryLL"));
				costIssueSumLL        = costIssueSumLL.add((BigDecimal)sonProject.get_Value("CostIssueSumLL")==null              ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostIssueSumLL"));
				costDiffExcecutionLL  = costDiffExcecutionLL.add((BigDecimal)sonProject.get_Value("CostDiffExcecutionLL")==null  ?Env.ZERO:(BigDecimal)sonProject.get_Value("CostDiffExcecutionLL"));
			}
			// Set Low Level Amounts
			project.set_ValueOfColumn("CostPlannedLL",         costPlannedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostAmtLL",             costAmtLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostNotInvoicedLL",     costNotInvoicedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostExtrapolatedLL",    costExtrapolatedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("RevenuePlannedLL",      revenuePlannedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("RevenueAmtLL",          revenueAmtLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("RevenueNotInvoicedLL",  revenueNotInvoicedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("RevenueExtrapolatedLL", revenueExtrapolatedLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostIssueProductLL",    costIssueProductLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostIssueResourceLL",   costIssueResourceLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostIssueInventoryLL",  costIssueInventoryLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostIssueSumLL",        costIssueSumLL.setScale(2, RoundingMode.HALF_UP));
			project.set_ValueOfColumn("CostDiffExcecutionLL",  costDiffExcecutionLL.setScale(2, RoundingMode.HALF_UP));

			// Gross margin LL = extrapolated revenue LL - (extrapolated costs LL + resource issue costs LL + inventory issue costs LL)
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).
					subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if(grossMarginLL==null)
				grossMarginLL = Env.ZERO;
			project.set_ValueOfColumn("GrossMarginLL",grossMarginLL.setScale(2, RoundingMode.HALF_UP));		

			project.saveEx();  // Low Level Amounts
		}


		int C_Project_Parent_ID = project.get_ValueAsInt("C_Project_Parent_ID");  // Father Project -if any
		if (C_Project_Parent_ID!= 0)	{	
			// Project is child: update direct parent project
	    	MProject fatherProject = new MProject(project.getCtx(), C_Project_Parent_ID, project.get_TrxName());
			result = calcCostOrRevenuePlannedSons(C_Project_Parent_ID, false, true);      // Planned costs from Purchase Orders of all children of parent project
			fatherProject.set_ValueOfColumn("CostPlannedLL", result.setScale(2, RoundingMode.HALF_UP));
			result = calcCostOrRevenueActualSons(C_Project_Parent_ID, false, true);       // Actual costs from Purchase Invoices of all children of parent project
			fatherProject.set_ValueOfColumn("CostAmtLL", result.setScale(2, RoundingMode.HALF_UP));
			result = calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, false, true); // Sum of actual costs and planned (not yet invoiced) costs of all children of parent project
			fatherProject.set_ValueOfColumn("CostExtrapolatedLL", result.setScale(2, RoundingMode.HALF_UP));		
			
			fatherProject.saveEx();

	    	BigDecimal costActualFather       = (BigDecimal)fatherProject.get_Value("CostAmt");
	    	BigDecimal costPlannedFather      = (BigDecimal)fatherProject.get_Value("CostPlanned");
	    	BigDecimal costExtrapolatedFather = (BigDecimal)fatherProject.get_Value("CostExtrapolated");

	    	BigDecimal revenueAmtSons         = calcCostOrRevenueActualSons(C_Project_Parent_ID, true, true);	
	    	BigDecimal revenuePlannedSons     = calcCostOrRevenuePlannedSons(C_Project_Parent_ID, true, true);
	    	BigDecimal revenueAllExtrapolated = calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, true, true);	
	    	
	    	BigDecimal weightFather = (BigDecimal)fatherProject.get_Value("Weight");
	    	BigDecimal volumeFather = (BigDecimal)fatherProject.get_Value("Volume");
	    	
	    	
			List<MProject> projectsOfFather = new Query(project.getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", project.get_TrxName())
			.setParameters(C_Project_Parent_ID)
			.list();
			// Update all children of parent project
			for (MProject sonProject: projectsOfFather)	{
				BigDecimal revenueExtrapolatedSon = 
						(BigDecimal)sonProject.get_Value("RevenueExtrapolated");
				BigDecimal weight = (BigDecimal)sonProject.get_Value("Weight");
				BigDecimal volume = (BigDecimal)sonProject.get_Value("volume");
				if (volume == null)
					volume = Env.ZERO;
				BigDecimal shareRevenue = Env.ZERO;
				BigDecimal shareWeight  = Env.ZERO;
				BigDecimal shareVolume  = Env.ZERO;
				if (revenueExtrapolatedSon!=null && revenueAllExtrapolated.longValue()!= 0)
					shareRevenue = revenueExtrapolatedSon.divide(revenueAllExtrapolated, 5, RoundingMode.DOWN);
				if (weight!=null && weightFather != null && weightFather.longValue()!=0)
					shareWeight = weight.divide(weightFather, 5, RoundingMode.DOWN);
				if (volume!=null && volumeFather != null && volumeFather.longValue()!= 0)
					shareVolume = volume.divide(volumeFather, 5, RoundingMode.DOWN);
				calcCostPlannedInherited(sonProject, costPlannedFather,costActualFather,costExtrapolatedFather, shareVolume, shareWeight, shareRevenue);				
			}
			fatherProject.set_ValueOfColumn("RevenuePlannedLL", revenuePlannedSons.setScale(2, RoundingMode.HALF_UP));
			fatherProject.set_ValueOfColumn("RevenueAmtLL", revenueAmtSons.setScale(2, RoundingMode.HALF_UP));
			fatherProject.set_ValueOfColumn("RevenueExtrapolatedLL", revenueAllExtrapolated.setScale(2, RoundingMode.HALF_UP));
			fatherProject.saveEx();
			project.saveEx();
		}

		BigDecimal grossMarginTotal = ((BigDecimal)project.get_Value("GrossMargin")).add(grossMarginLL);
		if(grossMarginTotal==null)
			grossMarginTotal = Env.ZERO;
		project.set_ValueOfColumn("GrossMarginTotal", grossMarginTotal.setScale(2, RoundingMode.HALF_UP));
		
		Date date = new Date();
		long time = date.getTime();
		Timestamp timestamp = new Timestamp(time);
		project.set_ValueOfColumn("DateLastRun", timestamp);
		project.saveEx();
		
		return "";
	
		
	}

	/**
	 * Calculates this Project's Line Net Amount
	 * For phases and tasks with products
	 *	@return sum of Line Net Amount of all phases and tasks 
	 */
    private BigDecimal calcLineNetAmt() {		
    	StringBuffer sql = new StringBuffer();
    	sql.append("select sum (linenetamt) ");
    	sql.append("from c_project_calculate_price ");
    	sql.append("where C_Project_ID=?");
    	
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(projectID);
		
    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }
    

	/**
	 * Calculates this Project's Actual Amount
	 * For phases and tasks with products
	 *	@return sum of Actual Amount of all phases and tasks 
	 */
    private BigDecimal calcActualamt() {		
    	StringBuffer sql = new StringBuffer();
    	sql.append("select sum (actualamt) ");
    	sql.append("from c_project_calculate_price ");
    	sql.append("where C_Project_ID=?");
    	
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(projectID);
		
    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }

	/**
	 * Calculates a Project's Costs of Product Issues
	 * Out of Cost Detail
	 * @param c_Project_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return sum of Costs of Product Issues of all phases and tasks 
	 */
    private BigDecimal calcCostIssueProduct(int c_Project_ID, boolean isParentProject) {		
    	StringBuffer sql = new StringBuffer();
    	sql.append("SELECT COALESCE(SUM(cd.CostAmt + cd.CostAmtLL + cd.CostAdjustment + cd.CostAdjustmentLL),0) ");
    	sql.append("FROM C_ProjectIssue pi ");
    	sql.append("INNER JOIN M_CostDetail cd ON pi.c_ProjectIssue_ID=cd.c_ProjectIssue_ID ");

    	if(isParentProject)
    		sql.append("WHERE pi.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) " );
    	else
    	    sql.append("WHERE pi.C_Project_ID=? ");
    	   	
    	sql.append("AND pi.M_InOutLine_ID IS NOT NULL ");
    	
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		
    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }

	/**
	 * Calculates a Project's Costs of Product Issues
	 * Out of Project Lines
	 * @param c_Project_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return sum of Costs of Product Issues of all phases and tasks 
	 */
    private BigDecimal calcCostIssueResource(int c_Project_ID, boolean isParentProject) {		
    	StringBuffer sql = new StringBuffer();
    	sql.append("SELECT SUM (pl.committedamt) ");
    	sql.append("FROM c_projectline pl ");
    	sql.append("INNER JOIN c_project p ON (pl.c_project_id=p.c_project_id) ");

    	if(isParentProject)
    		sql.append("WHERE pl.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) " );
    	else
    	    sql.append("WHERE pl.C_Project_ID=? ");
    	
    	sql.append("AND pl.c_projectissue_ID IS NOT NULL ");
    	sql.append("AND pl.s_timeexpenseline_ID IS NOT NULL ");
    	
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		
    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }   

	/**
	 * Calculates a Project's Costs of Inventory Issues
	 * @param c_Project_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 * Out of Cost Detail
	 *	@return sum of Costs of Inventory Issues of all phases and tasks 
	 */
    private BigDecimal calcCostIssueInventory(int c_Project_ID, boolean isParentProject) {		
    	StringBuffer sql = new StringBuffer();
    	sql.append("SELECT COALESCE(SUM(cd.CostAmt + cd.CostAmtLL + cd.CostAdjustment + cd.CostAdjustmentLL),0) ");
    	sql.append("FROM C_ProjectIssue pi ");
    	sql.append("INNER JOIN M_CostDetail cd ON pi.c_ProjectIssue_ID=cd.c_ProjectIssue_ID ");

    	if(isParentProject)
    		sql.append("WHERE pi.C_Project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) " );
    	else
    	    sql.append("WHERE pi.C_Project_ID=? ");
    	
    	sql.append("AND pi.M_InOutLine_ID IS  NULL ");
    	sql.append("AND pi.s_timeexpenseline_ID IS NULL ");
    	
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);
		
    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }   
    
	/**
	 * Calculates Costs of Product Issues for this Project's children 
	 * Out of Cost Detail
	 * @param c_project_parent_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return sum of Costs of Product Issues of all phases and tasks of the project's children
	 */
    private BigDecimal calcCostIssueProductSons(int c_Project_Parent_ID, boolean isParentProject) {	
    	BigDecimal result = calcCostIssueProduct(c_Project_Parent_ID, isParentProject);
    	return result==null?Env.ZERO:result;
    }  
    
	/**
	 * Calculates Costs of Resource Issues for this Project's children 
	 * Out of Project Lines
	 * @param c_project_parent_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return sum of Costs of Resource Issues of all phases and tasks of the project's children
	 */
    private BigDecimal calcCostIssueResourceSons(int c_Project_Parent_ID, boolean isParentProject) {		
    	BigDecimal result = calcCostIssueResource(c_Project_Parent_ID, isParentProject);
    	return result==null?Env.ZERO:result;
    }  
    
	/**
	 * Calculates Costs of Inventory Issues for this Project's children 
	 * Out of Cost Detail
	 * @param c_project_parent_ID Project ID
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return sum of Costs of Inventory Issues of all phases and tasks of the project's children
	 */
    private BigDecimal calcCostIssueInventorySons(int c_Project_Parent_ID, boolean isParentProject) {	
    	BigDecimal result = calcCostIssueInventory(c_Project_Parent_ID, isParentProject);
    	return result==null?Env.ZERO:result;
    }
	
	/**
	 * 	Update Costs and Revenues in the following order 
	 * 1.- For the children of the project up to the lowest levels
	 * 2.- For a given Project 
	 * To avoid recursive relations, it breaks after 5 loops.
	 * @param levelCount depth of the call
	 *	@return message
	 */	
    private String updateProjectPerformanceCalculationSons(int c_Project_ID, int levelCount) {	

		if (levelCount == 5)  // For now, allow for 5 level depth
			return"";         // Right now, there is no verification of circular reference
		
    	String whereClause = "C_Project_Parent_ID=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(c_Project_ID);

		List<MProject> childrenProjects = new Query(project.getCtx(), MProject.Table_Name, whereClause, project.get_TrxName())
		.setParameters(params)
		.list();

		if (childrenProjects == null) {	
			// No children -> just update this project
			updateProjectPerformanceCalculation();
			return"";	
		}
		
		for (MProject childProject:childrenProjects) {
			// update all children of this child
			updateProjectPerformanceCalculationSons(childProject.getC_Project_ID(), levelCount+1);
		}
		// last but not least, update this project
		updateProjectPerformanceCalculation();
		return"";
    }
    /**
	 * Calculates this Project's Planned Costs or Revenue based on Orders
	 * Special treatment for closed orders
	 * @param c_Project_ID Project ID
	 * @param isSOTrx  (boolean) true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
    private BigDecimal calcCostOrRevenuePlanned(int c_Project_ID, boolean isSOTrx, boolean isParentProject) {
    	StringBuffer sql = new StringBuffer();
    	sql.append("SELECT COALESCE (SUM( ");
    	sql.append("CASE ");
    	sql.append("     WHEN pl.istaxincluded = 'Y' ");
    	sql.append("     THEN ");
    	sql.append("         CASE ");
    	sql.append("         WHEN o.docstatus in ('CL') ");
    	sql.append("         THEN ((ol.qtyinvoiced * ol.priceactual)- (ol.qtyinvoiced * ol.priceactual)/(1+(t.rate/100)))  ");
    	sql.append("         ELSE (ol.linenetamt- ol.linenetamt/(1+(t.rate/100))) ");
    	sql.append("         END ");
    	sql.append("     ELSE ");
    	sql.append("         CASE ");
    	sql.append("         WHEN o.docstatus IN ('CL') ");
    	sql.append("         THEN (ol.qtyinvoiced * ol.priceactual) ");
    	sql.append("         ELSE (ol.linenetamt) ");
    	sql.append("         END ");
    	sql.append("     END ");
    	sql.append("),0) ");
    	sql.append("FROM C_OrderLine ol ");
    	sql.append("INNER JOIN c_order o ON ol.c_order_ID = o.c_order_ID ");
    	sql.append("INNER JOIN m_pricelist pl ON o.m_pricelist_ID = pl.m_pricelist_ID ");
    	sql.append("INNER JOIN c_tax t ON ol.c_tax_ID = t.c_tax_ID ");
    	sql.append("WHERE ");
    	sql.append("o.c_order_ID IN  (select c_order_ID from c_order where docstatus in ('CO','CL','IP')   AND issotrx =  ? ) ");

    	if(isParentProject)
    		sql.append( "AND o.c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
    	else
    		sql.append("AND o.c_project_ID = ? ");

    	ArrayList<Object> params = new ArrayList<Object>();
    	params.add(isSOTrx);
    	params.add(c_Project_ID);

    	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
    	return result==null?Env.ZERO:result;
    }
    
	/**
	 * Calculates this Project's Actual Costs or Revenue based on Invoices
	 * @param c_Project_ID Project ID
	 * @param isSOTrx  (boolean) true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
    private BigDecimal calcCostOrRevenueActual(int c_Project_ID, boolean isSOTrx, boolean isParentProject) {
    	String expresion = "LineNetAmtRealInvoiceLine(c_invoiceline_ID)";
    	StringBuffer whereClause = new StringBuffer();
    	whereClause.append("c_invoice_ID IN (SELECT c_invoice_ID FROM c_invoice WHERE docstatus IN ('CO','CL') ");
    	whereClause.append(" AND issotrx = ");
    	whereClause.append(isSOTrx==true?  " 'Y') " : " 'N') ");

    	if(isParentProject)
    		whereClause.append( "AND c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
    	else
    		whereClause.append("AND c_project_ID = ? ");
    	
    	BigDecimal result = Env.ZERO;
    	result = new Query(project.getCtx(), MInvoiceLine.Table_Name, whereClause.toString(), project.get_TrxName())
    		.setParameters(c_Project_ID)
    		.aggregate(expresion, Query.AGGREGATE_SUM);
    	return result==null?Env.ZERO:result;
    }
	/**
	 * Calculates this Project's ordered, but not invoiced Costs or Revenue based on Orders
	 * Special treatment for closed orders
	 * @param c_Project_ID Project ID
	 * @param isSOTrx  (boolean) true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
   private BigDecimal calcNotInvoicedCostOrRevenue(int c_Project_ID, boolean isSOTrx, boolean isParentProject) {
   	StringBuffer sql = new StringBuffer();
   	sql.append("SELECT COALESCE (SUM( ");
   	sql.append("CASE ");
   	sql.append("     WHEN pl.istaxincluded = 'Y' ");
   	sql.append("     THEN ");
   	sql.append("         CASE ");
   	sql.append("         WHEN o.docstatus in ('CL') ");
   	sql.append("         THEN 0  ");
   	sql.append("         ELSE ((ol.qtyordered-ol.qtyinvoiced)*ol.Priceactual) - (taxamt_Notinvoiced(ol.c_Orderline_ID)) ");
   	sql.append("         END ");
   	sql.append("     ELSE ");
   	sql.append("         CASE ");
   	sql.append("         WHEN o.docstatus IN ('CL') ");
   	sql.append("         THEN 0 ");
   	sql.append("         ELSE ((ol.qtyordered-ol.qtyinvoiced)*ol.Priceactual) ");
   	sql.append("         END ");
   	sql.append("     END ");
   	sql.append("),0) ");
   	sql.append("FROM C_OrderLine ol ");
   	sql.append("INNER JOIN c_order o ON ol.c_order_ID = o.c_order_ID ");
   	sql.append("INNER JOIN m_pricelist pl ON o.m_pricelist_ID = pl.m_pricelist_ID ");
   	sql.append("INNER JOIN c_tax t ON ol.c_tax_ID = t.c_tax_ID ");
   	sql.append("WHERE ");
	sql.append("o.c_order_ID IN  (select c_order_ID from c_order where docstatus in ('CO','CL','IP')   AND issotrx =  ? ) ");

	if(isParentProject)
		sql.append( "AND o.c_project_ID IN (SELECT c_project_ID FROM c_project WHERE c_project_parent_ID =?) ");
	else
		sql.append("AND o.c_project_ID = ? ");
	
	ArrayList<Object> params = new ArrayList<Object>();
	params.add(isSOTrx);
	params.add(c_Project_ID);
		
   	BigDecimal result = DB.getSQLValueBDEx(null, sql.toString(), params);
   	return result==null?Env.ZERO:result;
    }
   
   /**
    * Calculates this Project's ordered, but not invoiced Costs or Revenue based on Order
	 *  added to this Project's Actual Costs or Revenue based on Invoices
	 * @param c_Project_ID Project ID
	 * @param isSOTrx  (boolean) true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
   private BigDecimal calcCostOrRevenueExtrapolated(int c_Project_ID, boolean isSOTrx, boolean isParentProject) {
    	BigDecimal result = calcNotInvoicedCostOrRevenue(c_Project_ID, isSOTrx, isParentProject).add(calcCostOrRevenueActual(c_Project_ID, isSOTrx, isParentProject));
    	return result==null?Env.ZERO:result;
    }


   /**
	 * Calculates Planned Costs or Revenue of a Project's direct children based on Orders
	 * It considers low level amounts.
	 * @param c_project_parent_ID Project ID
	 * @param isSOTrx boolean true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
   private BigDecimal calcCostOrRevenuePlannedSons(int c_Project_Parent_ID, boolean isSOTrx, boolean isParentProject) {
   	BigDecimal result = calcCostOrRevenuePlanned(c_Project_Parent_ID, isSOTrx, isParentProject);
   	return result==null?Env.ZERO:result;
   }
   

	/**
	 * Calculates Actual Costs or Revenue of a Project's direct children based on Invoices
	 * @param c_Project_Parent_ID Project ID
	 * @param isSOTrx boolean true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
   private BigDecimal calcCostOrRevenueActualSons(int c_Project_Parent_ID, boolean isSOTrx, boolean isParentProject) {
   	BigDecimal result = calcCostOrRevenueActual(c_Project_Parent_ID, isSOTrx, isParentProject);
   	return result==null?Env.ZERO:result;
   }     
   
   /**
	 * Calculates not Invoiced Costs or Revenue of a Project's direct children based on Orders
	 * @param c_Project_Parent_ID Project ID
	 * @param isSOTrx boolean true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
	 *	@return Amount of Cost or Revenue, depending on parameter 
	 */
    private BigDecimal calcNotInvoicedCostOrRevenueSons(int c_Project_Parent_ID, boolean isSOTrx, boolean isParentProject) {
    	BigDecimal result = calcNotInvoicedCostOrRevenue(c_Project_Parent_ID, isSOTrx, isParentProject);
    	return result==null?Env.ZERO:result;  
   }
    
    /**
     * Calculates not Invoiced Costs or Revenue of a Project's direct children based on Orders
 	 *  added to  Actual Costs or Revenue of a Project's children based on Invoices
	 * @param c_Project_Parent_ID Project ID
 	 * @param isSOTrx  (boolean) true (Revenue) or false (Cost)
	 * @param isParentProject  (boolean) true (Include all child Projects) or false (Only this Project)
 	 *	@return Amount of Cost or Revenue, depending on parameter 
 	 */
    private BigDecimal calcCostOrRevenueExtrapolatedSons(int c_Project_Parent_ID, boolean isSOTrx, boolean isParentProject) {
   	BigDecimal result = calcNotInvoicedCostOrRevenueSons(c_Project_Parent_ID, isSOTrx, isParentProject).add(calcCostOrRevenueActualSons(c_Project_Parent_ID, isSOTrx, isParentProject));
   	return result==null?Env.ZERO:result;
   }    


    private Boolean calcCostPlannedInherited(MProject son, BigDecimal costPlannedFather
    		, BigDecimal costActualFather
    		, BigDecimal costExtrapolatedFather
    		, BigDecimal shareVolume
    		, BigDecimal shareWeight
    		, BigDecimal shareRevenue)
    {
    	if(son==null)
        	return true;
    	if(costPlannedFather==null)
    		costPlannedFather = Env.ZERO;
    	if(costActualFather==null)
    		costActualFather = Env.ZERO;
    	if(costExtrapolatedFather==null)
    		costExtrapolatedFather = Env.ZERO;
    	if(shareVolume==null)
    		shareVolume = Env.ZERO;
    	if(shareWeight==null)
    		shareWeight = Env.ZERO;
    	if(shareRevenue==null)
    		shareRevenue = Env.ZERO;
    	
    	BigDecimal result = Env.ZERO;
    	result = costPlannedFather.multiply(shareRevenue);
    	son.set_ValueOfColumn("CostPlannedInherited", result);
    	result = costPlannedFather.multiply(shareVolume);
    	son.set_ValueOfColumn("CostPlannedVolumeInherited", result);
    	result = costPlannedFather.multiply(shareWeight);
    	son.set_ValueOfColumn("CostPlannedWeightInherited", result);

    	result = costActualFather.multiply(shareRevenue);
    	son.set_ValueOfColumn("CostAmtInherited", result);
    	result = costActualFather.multiply(shareVolume);
    	son.set_ValueOfColumn("CostAmtVolumeInherited", result);
    	result = costActualFather.multiply(shareWeight);
    	son.set_ValueOfColumn("CostAmtWeightInherited", result);
    	
    	result = costExtrapolatedFather.multiply(shareRevenue);
    	son.set_ValueOfColumn("CostExtrapolatedInherited", result);
    	result = costExtrapolatedFather.multiply(shareVolume);
    	son.set_ValueOfColumn("CostExtrapolatedVolInherited", result);
    	result = costExtrapolatedFather.multiply(shareWeight);
    	son.set_ValueOfColumn("CostExtrapolatedWghtInherited", result);
    	
    	if (son.getC_Project_ID() != projectID)
    		son.saveEx();    	
    	return true;
    }
    
    
   





}
