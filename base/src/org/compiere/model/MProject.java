/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.compiere.util.CCache;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MProjectMember;
import org.eevolution.model.X_I_Project;

/**
 * 	Project Model
 *
 *	@author Jorg Janke
 *  @author Víctor Pérez Juárez , victor.perez@e-evolution.com , http://www.e-evolution.com
 *  <a href="https://github.com/adempiere/adempiere/issues/1478">
 *  <li>Add support to create request based on Standard Request Type setting on Project Type #1478
 *	@version $Id: MProject.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 *	@author Carlos Parada, cparada@erpya.com, ERPCyA http://www.erpya.com
 *  	<a href="https://github.com/adempiere/adempiere/issues/2117">
 *		@see FR [ 2117 ] Add Support to Price List on Project</a>
 *	@author Mario Calderon, mario.calderon@westfalia-it.com, Systenmhaus Westfalia http://www.westfalia-it.com
 */
public class MProject extends X_C_Project
{
	private static CCache<Integer, MProject> projectCacheIds = new CCache<Integer, MProject>(Table_Name, 100, 0);
	private static CCache<String, MProject> projectCacheValues = new CCache<String, MProject>(Table_Name, 100, 0);

	/**
	 * Ge project by Id
	 * @param ctx
	 * @param projectId
	 * @param trxName
	 * @return
	 */
	public static MProject getById(Properties ctx, Integer projectId, String trxName) {
		if (projectId <= 0)
			return null;
		if (projectCacheIds.size() == 0)
			getAll(ctx, true, trxName);

		MProject project = projectCacheIds.get(projectId);
		if (project != null)
			return project;

		project =  new Query(ctx , Table_Name , COLUMNNAME_C_Project_ID + "=?" , null)
				.setClient_ID()
				.setParameters(projectId)
				.first();

		if (project != null && project.get_ID() > 0) {
			int clientId = Env.getAD_Client_ID(ctx);
			String key = clientId + "#" + project.getValue();
			projectCacheIds.put(project.get_ID(), project);
			projectCacheValues.put(key, project);
		}
		return project;
	}

	/**
	 * Get project by Search Key
	 * @param ctx
	 * @param value
	 * @param trxName
	 * @return
	 */
	public static MProject getByValue(Properties ctx, String value, String trxName) {
		if (value == null)
			return null;
		if (projectCacheValues.size() == 0)
			getAll(ctx, true, trxName);

		int clientId = Env.getAD_Client_ID(ctx);
		String key = clientId + "#" + value;
		MProject project = projectCacheValues.get(key);
		if (project != null && project.get_ID() > 0)
			return project;

		project = new Query(ctx, Table_Name, COLUMNNAME_Value + "=?", trxName)
				.setClient_ID()
				.setParameters(value)
				.first();
		if (project != null && project.get_ID() > 0) {
			projectCacheValues.put(key, project);
			projectCacheIds.put(project.get_ID(), project);
		}
		return project;
	}

	/**
	 * Get all project and create cache
	 * @param ctx
	 * @param resetCache
	 * @param trxName
	 * @return
	 */
	public static List<MProject> getAll(Properties ctx, boolean resetCache, String trxName) {
		List<MProject> projectList;
		if (resetCache || projectCacheIds.size() > 0) {
			projectList = new Query(Env.getCtx(), Table_Name, null, trxName)
					.setClient_ID()
					.setOrderBy(COLUMNNAME_Name)
					.list();
			projectList.stream().forEach(project -> {
				int clientId = Env.getAD_Client_ID(ctx);
				String key = clientId + "#" + project.getValue();
				projectCacheIds.put(project.getC_Project_ID(), project);
				projectCacheValues.put(key, project);
			});
			return projectList;
		}
		projectList = projectCacheIds.entrySet().stream()
				.map(project -> project.getValue())
				.collect(Collectors.toList());
		return projectList;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2151648902207548617L;

	/**
	 * 	Create new Project by copying
	 * 	@param ctx context
	 *	@param C_Project_ID project
	 * 	@param dateDoc date of the document date
	 *	@param trxName transaction
	 *	@return Project
	 */
	public static MProject copyFrom (Properties ctx, int C_Project_ID, Timestamp dateDoc, String trxName)
	{
		MProject from = new MProject (ctx, C_Project_ID, trxName);
		if (from.getC_Project_ID() == 0)
			throw new IllegalArgumentException ("From Project not found C_Project_ID=" + C_Project_ID);
		//
		MProject to = new MProject (ctx, 0, trxName);
		PO.copyValues(from, to, from.getAD_Client_ID(), from.getAD_Org_ID());
		to.set_ValueNoCheck ("C_Project_ID", I_ZERO);
		//	Set Value with Time
		String Value = to.getValue() + " ";
		String Time = dateDoc.toString();
		int length = Value.length() + Time.length();
		if (length <= 40)
			Value += Time;
		else
			Value += Time.substring (length-40);
		to.setValue(Value);
		to.setInvoicedAmt(Env.ZERO);
		to.setProjectBalanceAmt(Env.ZERO);
		to.setProcessed(false);
		//
		if (!to.save())
			throw new IllegalStateException("Could not create Project");

		if (to.copyDetailsFrom(from) == 0)
			throw new IllegalStateException("Could not create Project Details");

		return to;
	}	//	copyFrom

	
	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param C_Project_ID id
	 *	@param trxName transaction
	 */
	public MProject (Properties ctx, int C_Project_ID, String trxName)
	{
		super (ctx, C_Project_ID, trxName);
		if (C_Project_ID == 0)
		{
		//	setC_Project_ID(0);
		//	setValue (null);
		//	setC_Currency_ID (0);
			setCommittedAmt (Env.ZERO);
			setCommittedQty (Env.ZERO);
			setInvoicedAmt (Env.ZERO);
			setInvoicedQty (Env.ZERO);
			setPlannedAmt (Env.ZERO);
			setPlannedMarginAmt (Env.ZERO);
			setPlannedQty (Env.ZERO);
			setProjectBalanceAmt (Env.ZERO);
		//	setProjectCategory(PROJECTCATEGORY_General);
			setProjInvoiceRule(PROJINVOICERULE_None);
			setProjectLineLevel(PROJECTLINELEVEL_Project);
			setIsCommitCeiling (false);
			setIsCommitment (false);
			setIsSummary (false);
			setProcessed (false);
		}
	}	//	MProject

	public MProject (X_I_Project projectImport)
	{
		super(projectImport.getCtx() , 0 , projectImport.get_TrxName());
		setAD_Org_ID(projectImport.getAD_Org_ID());
		setM_PriceList_Version_ID(projectImport.getM_PriceList_Version_ID());
		setAD_Color_ID(projectImport.getAD_Color_ID());
		setAD_OrgTrx_ID(projectImport.getAD_OrgTrx_ID());
		setAD_User_ID(projectImport.getAD_User_ID());
		setC_Activity_ID(projectImport.getC_Activity_ID());
		setC_BPartner_ID(projectImport.getC_BPartner_ID());
		setC_BPartner_Location_ID(projectImport.getC_BPartner_Location_ID());
		setC_BPartnerSR_ID(projectImport.getC_BPartnerSR_ID());
		setC_Campaign_ID(projectImport.getC_Campaign_ID());
		setC_Currency_ID(projectImport.getC_Currency_ID());
		setC_PaymentTerm_ID(projectImport.getC_PaymentTerm_ID());
		setC_PaymentTerm_ID(projectImport.getC_PaymentTerm_ID());
		setC_ProjectCategory_ID(projectImport.getC_ProjectCategory_ID());
		setC_ProjectClass_ID(projectImport.getC_ProjectClass_ID());
		setC_ProjectGroup_ID(projectImport.getC_ProjectGroup_ID());
		setC_ProjectStatus_ID(projectImport.getC_ProjectStatus_ID());
		setC_SalesRegion_ID(projectImport.getC_SalesRegion_ID());
		setCommittedAmt(projectImport.getCommittedAmt());
		setCommittedQty(projectImport.getCommittedQty());
		setDateContract(projectImport.getDateContract());
		setDateDeadline(projectImport.getDateDeadline());
		setDateFinish(projectImport.getDateFinish());
		setDateStart(projectImport.getDateStart());
		setDateFinishSchedule(projectImport.getDateFinishSchedule());
		setDateStartSchedule(projectImport.getDateStartSchedule());
		setDescription(projectImport.getDescription());
		setDurationUnit(projectImport.getDurationUnit());
		setInvoicedAmt(projectImport.getInvoicedAmt());
		setInvoicedQty(projectImport.getInvoicedQty());
		setIsCommitCeiling(projectImport.isCommitCeiling());
		setIsCommitment(isCommitment());
		setIsIndefinite(projectImport.isIndefinite());
		setIsSummary(projectImport.isSummary());
		setM_Warehouse_ID(projectImport.getM_Warehouse_ID());
		setName(projectImport.getName());
		setNote(projectImport.getNote());
		setPlannedAmt(projectImport.getPlannedAmt());
		setPlannedMarginAmt(projectImport.getPlannedMarginAmt());
		setPlannedQty(projectImport.getPlannedQty());
		setPOReference(projectImport.getPOReference());
		setPriorityRule(projectImport.getPriorityRule());
		setProjectBalanceAmt(projectImport.getProjectBalanceAmt());
		setProjectLineLevel(projectImport.getProjectLineLevel());
		setProjectManager_ID(projectImport.getProjectManager_ID());
		setProjInvoiceRule(projectImport.getProjInvoiceRule());
		setSalesRep_ID(projectImport.getSalesRep_ID());
		setUser1_ID(projectImport.getUser1_ID());
		setUser2_ID(projectImport.getUser2_ID());
		setUser3_ID(projectImport.getUser3_ID());
		setUser4_ID(projectImport.getUser4_ID());
		setValue(projectImport.getValue());
	}


	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MProject (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MProject

	/**	Cached PL			*/
	private int		m_M_PriceList_ID = 0;

	/**
	 * 	Get Project Type as Int (is Button).
	 *	@return C_ProjectType_ID id
	 */
	public int getC_ProjectType_ID_Int()
	{
		String pj = super.getC_ProjectType_ID();
		if (pj == null)
			return 0;
		int C_ProjectType_ID = 0;
		try
		{
			C_ProjectType_ID = Integer.parseInt (pj);
		}
		catch (Exception ex)
		{
			log.log(Level.SEVERE, pj, ex);
		}
		return C_ProjectType_ID;
	}	//	getC_ProjectType_ID_Int

	/**
	 * 	Set Project Type (overwrite r/o)
	 *	@param C_ProjectType_ID id
	 */
	public void setC_ProjectType_ID (int C_ProjectType_ID)
	{
		if (C_ProjectType_ID == 0)
			super.setC_ProjectType_ID (null);
		else
			super.set_Value("C_ProjectType_ID", C_ProjectType_ID);
	}	//	setC_ProjectType_ID

	/**
	 *	String Representation
	 * 	@return info
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer ("MProject[").append(get_ID())
			.append("-").append(getValue()).append(",ProjectCategory=").append(getProjectCategory())
			.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * 	Get Price List from Price List Version
	 *	@return price list or 0
	 */
	public int getM_PriceList_ID()
	{
		//FR [ 2117 ]
		if (super.getM_PriceList_ID()==0 && getM_PriceList_Version_ID() == 0)
			return 0;
		
		if (super.getM_PriceList_ID()!=0)
			m_M_PriceList_ID = super.getM_PriceList_ID();
		
		if (m_M_PriceList_ID > 0)
			return m_M_PriceList_ID;
		//
		String sql = "SELECT M_PriceList_ID FROM M_PriceList_Version WHERE M_PriceList_Version_ID=?";
		m_M_PriceList_ID = DB.getSQLValue(null, sql, getM_PriceList_Version_ID());
		return m_M_PriceList_ID;
	}	//	getM_PriceList_ID

	/**
	 * 	Set PL Version
	 *	@param M_PriceList_Version_ID id
	 */
	public void setM_PriceList_Version_ID (int M_PriceList_Version_ID)
	{
		super.setM_PriceList_Version_ID(M_PriceList_Version_ID);
		m_M_PriceList_ID = 0;	//	reset
	}	//	setM_PriceList_Version_ID


	/**************************************************************************
	 * 	Get Project Lines
	 *	@return Array of lines
	 */
	public List<MProjectLine> getLines()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		final String whereClause = "C_Project_ID=?";
		return new Query(getCtx(), I_C_ProjectLine.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line")
			.list();
	}	//	getLines

	/**
	 * 	Get Project Issues
	 *	@return Array of issues
	 */
	public List<MProjectIssue> getIssues()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		return new Query(getCtx(), I_C_ProjectIssue.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("Line")
			.list();
	}	//	getIssues

	/**
	 * 	Get Project Phases
	 *	@return Array of phases
	 */
	public List<MProjectPhase> getPhases()
	{
		//FR: [ 2214883 ] Remove SQL code and Replace for Query - red1
		String whereClause = "C_Project_ID=?";
		return new Query(getCtx(), I_C_ProjectPhase.Table_Name, whereClause, get_TrxName())
			.setParameters(getC_Project_ID())
			.setOrderBy("SeqNo")
			.list();
	}	//	getPhases

	
	/**************************************************************************
	 * 	Copy Lines/Phase/Task from other Project
	 *	@param project project
	 *	@return number of total lines copied
	 */
	public int copyDetailsFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		int count = copyLinesFrom(project)
			+ copyPhasesFrom(project);
		return count;
	}	//	copyDetailsFrom

	/**
	 * 	Copy Lines From other Project
	 *	@param project project
	 *	@return number of lines copied
	 */
	public int copyLinesFrom (MProject project)
	{
		if (isProcessed() || project == null)
			return 0;
		AtomicInteger count = new AtomicInteger(0);
		List<MProjectLine> fromProjectLines = project.getLines();
		fromProjectLines.stream()
				.filter(fromProjectLine ->
						fromProjectLine.getC_ProjectPhase_ID() <= 0
					 || fromProjectLine.getC_ProjectTask_ID() <= 0)
				.forEach(fromProjectLine -> {
					MProjectLine toProjectLine = new MProjectLine(getCtx(), 0, project.get_TrxName());
					PO.copyValues(fromProjectLine, toProjectLine, getAD_Client_ID(), getAD_Org_ID());
					toProjectLine.setC_Project_ID(getC_Project_ID());
					toProjectLine.setInvoicedAmt(Env.ZERO);
					toProjectLine.setInvoicedQty(Env.ZERO);
					toProjectLine.setC_OrderPO_ID(0);
					toProjectLine.setC_Order_ID(0);
					toProjectLine.setProcessed(false);
					toProjectLine.saveEx();
					count.getAndUpdate(no -> no + 1);
				});

		if (fromProjectLines.size() != count.get())
			log.log(Level.SEVERE, "Lines difference - Project=" + fromProjectLines.size() + " <> Saved=" + count);
		return count.get();
	}	//	copyLinesFrom

	/**
	 * 	Copy Phases/Tasks from other Project
	 *	@param fromProject project
	 *	@return number of items copied
	 */
	public int copyPhasesFrom (MProject fromProject)
	{
		if (isProcessed() || fromProject == null)
			return 0;
		AtomicInteger count = new AtomicInteger(0);
		AtomicInteger taskCount = new AtomicInteger(0);
		AtomicInteger lineCount = new AtomicInteger(0);
		//	Get Phases
		List<MProjectPhase> toPhases = getPhases();
		List<MProjectPhase> fromPhases = fromProject.getPhases();
		fromPhases.stream()
				.forEach(fromPhase -> {
					//	Check if Phase already exists
					Boolean exists = toPhases.stream().anyMatch(toPhase -> toPhase.getC_Phase_ID() == fromPhase.getC_Phase_ID());
					//	Phase exist
					if (exists)
						log.info("Phase already exists here, ignored - " + fromPhase);
					else {
						MProjectPhase toPhase = new MProjectPhase(getCtx(), 0, get_TrxName());
						PO.copyValues(fromPhase, toPhase, getAD_Client_ID(), getAD_Org_ID());
						toPhase.setC_Project_ID(getC_Project_ID());
						toPhase.setC_Order_ID(0);
						toPhase.setIsComplete(false);
						toPhase.saveEx();
						count.getAndUpdate(no -> no + 1);
						taskCount.getAndUpdate(taskNo -> taskNo + toPhase.copyTasksFrom(fromPhase));
						lineCount.getAndUpdate(lineNo -> lineNo + toPhase.copyLinesFrom(fromPhase));
					}
				});
		if (fromPhases.size() != count.get())
			log.warning("Count difference - Project=" + fromPhases.size() + " <> Saved=" + count.get());

		return count.get() + taskCount.get() + lineCount.get();
	}	//	copyPhasesFrom


	/**
	 *	Set Project Type and Category.
	 * 	If Service Project copy Project Type Phase/Tasks
	 *	@param type project type
	 */
	public void setProjectType (MProjectType type)
	{
		if (type == null)
			return;
		setC_ProjectType_ID(Integer.toString(type.getC_ProjectType_ID()));
		createRequest(type);
		copyPhasesFrom(type);
	}	//	setProjectType


	/**
	 *	Copy Phases from Type
	 *	@param type Project Type
	 *	@return count
	 */
	public int copyPhasesFrom (MProjectType type)
	{
		//	create phases
		AtomicInteger count = new AtomicInteger(0);
		AtomicInteger taskCount = new AtomicInteger(0);
		List<MProjectTypePhase> typePhases = type.getPhases();
		typePhases.stream()
				.forEach(fromPhase -> {
					MProjectPhase toPhase = new MProjectPhase(this, fromPhase);
					toPhase.setC_Project_ID(getC_Project_ID());
					toPhase.setProjInvoiceRule(getProjInvoiceRule());
					toPhase.saveEx();
					count.getAndUpdate(no -> no + 1);
					taskCount.getAndUpdate(no -> no + toPhase.copyTasksFrom(fromPhase));
				});
		log.fine("#" + count.get() + "/" + taskCount.get()
			+ " - " + type);
		if (typePhases.size() != count.get())
			log.log(Level.SEVERE, "Count difference - Type=" + typePhases.size() + " <> Saved=" + count.get());
		return count.get();
	}	//	copyPhasesFrom


	/**
	 * create Request Project
	 */
	public void createRequest(MProjectType projectType)
	{
		if (projectType.getR_StandardRequestType_ID() > 0)
		{
			MStandardRequestType standardRequestType = (MStandardRequestType) projectType.getR_StandardRequestType();
			List<MRequest> requests =  standardRequestType.createStandardRequest(this);
			requests.stream().forEach(request -> {
				request.setC_Project_ID(getC_Project_ID());
				request.setDateStartPlan(getDateStartSchedule());
				request.setDateCompletePlan(getDateFinishSchedule());
				request.saveEx();
			});
		}
	}

	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		if (getAD_User_ID() == -1)	//	Summary Project in Dimensions
			setAD_User_ID(0);
		
		//	Set Currency
		if (is_ValueChanged("M_PriceList_Version_ID") && getM_PriceList_Version_ID() != 0)
		{
			MPriceList pl = MPriceList.get(getCtx(), getM_PriceList_ID(), null);
			if (pl != null && pl.get_ID() != 0)
				setC_Currency_ID(pl.getC_Currency_ID());
		}
		
		if (is_ValueChanged("C_ProjectCategory_ID"))
			setProjectCategory(getC_ProjectCategory().getProjectCategory());
		
		return true;
	}	//	beforeSave
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (newRecord && success)
		{
			insert_Accounting("C_Project_Acct", "C_AcctSchema_Default", null);
		}

		//	Value/Name change
		if (success && !newRecord 
			&& (is_ValueChanged("Value") || is_ValueChanged("Name")))
			MAccount.updateValueDescription(getCtx(), "C_Project_ID=" + getC_Project_ID(), get_TrxName());

		if (getSalesRep_ID() > 0 && !MProjectMember.memberExists(this, getSalesRep_ID()))
			MProjectMember.addMember(this, getSalesRep_ID());
		if (getProjectManager_ID() > 0 && !MProjectMember.memberExists(this, getProjectManager_ID()))
			MProjectMember.addMember(this, getProjectManager_ID());
		if (getAD_User_ID() > 0 && !MProjectMember.memberExists(this, getAD_User_ID()))
			MProjectMember.addMember(this, getAD_User_ID());

		return success;
	}	//	afterSave

	/**
	 * 	Before Delete
	 *	@return true
	 */
	protected boolean beforeDelete ()
	{
		return delete_Accounting("C_Project_Acct"); 
	}	//	beforeDelete
	
	/**
	 * 	Return the Invoices Generated for this Project
	 *	@return invoices
	 *	@author monhate
	 */	
	public MInvoice[] getMInvoices(){
		StringBuilder sb = new StringBuilder();
		sb.append(MInvoice.COLUMNNAME_C_Project_ID).append("=?");
		Query qry = new Query(getCtx(), MInvoice.Table_Name, sb.toString(), get_TrxName());
		qry.setParameters(getC_Project_ID());		
		return (MInvoice[]) qry.list().toArray();
	}

	
	/**
	 * 	Update Costs and Revenues 
	 * 1.- For a given Project 
	 * 2.- For the children of the project
	 * 3.- For the parent project, if any
	 *	@return message
	 */	
	public String updateProjectPerformanceCalculation() {
		BigDecimal result = Env.ZERO;
		
		// Update Prices
		result = calcLineNetAmt();
		set_ValueOfColumn("ProjectPriceListRevenuePlanned", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcActualamt();
		set_ValueOfColumn("ProjectOfferedRevenuePlanned", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		
		// Update costs
		// Parameters:                     Project         isSOtrx  isParentProject
		result = calcCostOrRevenuePlanned(getC_Project_ID(), false, false);      // Planned costs from Purchase Orders this project
		set_ValueOfColumn("CostPlanned", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcCostOrRevenueActual(getC_Project_ID(), false, false); 		 // Actual costs from Purchase Invoices
		set_ValueOfColumn("CostAmt", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcNotInvoicedCostOrRevenue(getC_Project_ID(), false, false);  // Planned but not yet invoiced costs
		set_ValueOfColumn("CostNotInvoiced", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		BigDecimal costExtrapolated = calcCostOrRevenueExtrapolated(getC_Project_ID(), false, false); // Actual costs + Planned but not yet invoiced costs
		set_ValueOfColumn("CostExtrapolated", costExtrapolated.setScale(2, BigDecimal.ROUND_HALF_UP));

		// Update revenues
		result = calcCostOrRevenuePlanned(getC_Project_ID(), true, false);     // Planned revenue from Sales Orders this project
		set_ValueOfColumn("RevenuePlanned", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcCostOrRevenueActual(getC_Project_ID(), true, false); 	   // Actual revenue from Sales Invoices
		set_ValueOfColumn("RevenueAmt", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		result = calcNotInvoicedCostOrRevenue(getC_Project_ID(), true, false); // Planned but not yet invoiced revenue
		set_ValueOfColumn("RevenueNotInvoiced", result.setScale(2, BigDecimal.ROUND_HALF_UP));
		BigDecimal revenueExtrapolated = calcCostOrRevenueExtrapolated(getC_Project_ID(), true, false);  // Actual revenue + Planned but not yet invoiced revenue
		set_ValueOfColumn("RevenueExtrapolated", revenueExtrapolated.setScale(2, BigDecimal.ROUND_HALF_UP));
		
		// Update Issue Costs
		BigDecimal costIssueProduct = calcCostIssueProduct(getC_Project_ID(), false);		// Costs of Product Issues
		set_ValueOfColumn("CostIssueProduct", costIssueProduct.setScale(2, BigDecimal.ROUND_HALF_UP));
		BigDecimal costIssueResource = calcCostIssueResource(getC_Project_ID(), false);		// Costs of Resource Issues
		set_ValueOfColumn("CostIssueResource", costIssueResource.setScale(2, BigDecimal.ROUND_HALF_UP));
		BigDecimal costIssueInventory = calcCostIssueInventory(getC_Project_ID(), false);   // Costs of Inventory Issues
		set_ValueOfColumn("CostIssueInventory", costIssueInventory.setScale(2, BigDecimal.ROUND_HALF_UP));
		set_ValueOfColumn("CostIssueSum", costIssueProduct.add(costIssueResource).
				add(costIssueInventory).setScale(2, BigDecimal.ROUND_HALF_UP));  // Issue sum = Costs of Product Issue + Costs of Resource Issue + Costs of Inventory Issues
		set_ValueOfColumn("CostDiffExcecution", ((BigDecimal)get_Value("CostPlanned")).
				subtract(costIssueProduct).
				subtract(costIssueInventory).setScale(2, BigDecimal.ROUND_HALF_UP));  // Execution Diff = Planned Costs - (Product Issue Costs + Inventory Issue Costs

		// Gross Margin
		// Gross margin = extrapolated revenue - (extrapolated costs + resource issue costs + inventory issue costs)
		BigDecimal sumCosts = costExtrapolated.add(costIssueResource).add(costIssueInventory);
		
		BigDecimal grossMargin = revenueExtrapolated.subtract(sumCosts);
		set_ValueOfColumn("GrossMargin",grossMargin.setScale(2, BigDecimal.ROUND_HALF_UP));		

		// Margin (%) only for this level; there is no use to calculate it on LL
		if(sumCosts.compareTo(Env.ZERO)==0 && revenueExtrapolated.compareTo(Env.ZERO)==0) {
			set_ValueOfColumn("Margin", Env.ZERO); // Costs==0, Revenue== -> 0% margin	
			}
		else if(sumCosts.compareTo(Env.ZERO)!=0) {
			if(revenueExtrapolated.compareTo(Env.ZERO)!=0) {
				set_ValueOfColumn("Margin", revenueExtrapolated.divide(sumCosts, 6, BigDecimal.ROUND_HALF_UP).subtract(Env.ONE).
						multiply(Env.ONEHUNDRED).setScale(2, BigDecimal.ROUND_HALF_UP));
			}
			else {
				set_ValueOfColumn("Margin", Env.ONEHUNDRED.negate()); // Revenue==0 -> -100% margin						
			}

		} 
		else {
			set_ValueOfColumn("Margin", Env.ONEHUNDRED); // Costs==0 -> 100% margin			
		}
		
		BigDecimal grossMarginLL = Env.ZERO; // Gross Margin of children

		if (isSummary()) { // Project is a parent project
			// Update costs of direct children (not recursively all children!)
			BigDecimal costPlannedLL = calcCostOrRevenuePlannedSons(getC_Project_ID(), false, true);	       // Planned costs from Purchase Orders of children
			set_Value("CostPlannedLL", costPlannedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costAmtLL = calcCostOrRevenueActualSons(getC_Project_ID(), false, true);		           // Actual costs from Purchase Invoices of children
			set_Value("CostAmtLL", costAmtLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costNotInvoicedLL = calcNotInvoicedCostOrRevenueSons(getC_Project_ID(), false, true);   // Planned but not yet invoiced costs of children
			set_Value("CostNotInvoicedLL", costNotInvoicedLL.setScale(2, BigDecimal.ROUND_HALF_UP));	
			BigDecimal costExtrapolatedLL = calcCostOrRevenueExtrapolatedSons(getC_Project_ID(), false, true); // Actual costs + Planned but not yet invoiced costs of children
			set_Value("CostExtrapolatedLL", costExtrapolatedLL.setScale(2, BigDecimal.ROUND_HALF_UP));			
			
			// update revenues of children
			BigDecimal revenuePlannedLL = calcCostOrRevenuePlannedSons(getC_Project_ID(), true, true);	         // Planned revenue from Sales Orders of children
			set_ValueOfColumn("RevenuePlannedLL", revenuePlannedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal revenueAmtLL = calcCostOrRevenueActualSons(getC_Project_ID(), true, true);		         // Actual revenue from Sales Invoices of children
			set_ValueOfColumn("RevenueAmtLL", revenueAmtLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal revenueNotInvoicedLL = calcNotInvoicedCostOrRevenueSons(getC_Project_ID(), true, true);   // Planned but not yet invoiced revenue of children
			set_ValueOfColumn("RevenueNotInvoicedLL", revenueNotInvoicedLL.setScale(2, BigDecimal.ROUND_HALF_UP));	
			BigDecimal revenueExtrapolatedLL = calcCostOrRevenueExtrapolatedSons(getC_Project_ID(), true, true); // Actual revenue + Planned but not yet invoiced revenue of children
			set_ValueOfColumn("RevenueExtrapolatedLL", revenueExtrapolatedLL.setScale(2, BigDecimal.ROUND_HALF_UP));				
			
			// Update Issue Costs of children
			BigDecimal costIssueProductLL = calcCostIssueProductSons(getC_Project_ID(), true);      // Costs of Product Issues of children
			set_ValueOfColumn("CostIssueProductLL", costIssueProductLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costIssueResourceLL = calcCostIssueResourceSons(getC_Project_ID(), true);    // Costs of Resource Issues of children
			set_ValueOfColumn("CostIssueResourceLL", costIssueResourceLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costIssueInventoryLL = calcCostIssueInventorySons(getC_Project_ID(), true);  // Costs of Inventory Issues of children
			set_ValueOfColumn("CostIssueInventoryLL", costIssueInventoryLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costIssueSumLL  = costIssueProductLL.  // Issue sum LL = Costs of Product Issue LL + Costs of Resource Issue LL+ Costs of Inventory Issue LL
					add(costIssueResourceLL).add(costIssueInventoryLL).setScale(2, BigDecimal.ROUND_HALF_UP); 
			set_ValueOfColumn("CostIssueSumLL", costIssueSumLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			BigDecimal costDiffExcecutionLL  = costPlannedLL. // Execution Diff LL = Planned Costs LL - (Product Issue Costs LL + Inventory Issue Costs LL)
					subtract(costIssueProductLL).subtract(costIssueInventoryLL).setScale(2, BigDecimal.ROUND_HALF_UP);
			set_ValueOfColumn("CostDiffExcecutionLL", costDiffExcecutionLL.setScale(2, BigDecimal.ROUND_HALF_UP));

			// Gross margin LL = extrapolated revenue LL - (extrapolated costs LL + resource issue costs LL + inventory issue costs LL)
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).
					subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if(grossMarginLL==null)
				grossMarginLL = Env.ZERO;
			set_ValueOfColumn("GrossMarginLL",grossMarginLL.setScale(2, BigDecimal.ROUND_HALF_UP));

			saveEx();  // TODO: delete line

	    	BigDecimal costActualFather       = (BigDecimal)get_Value("CostAmt");
	    	BigDecimal costPlannedFather      = (BigDecimal)get_Value("CostPlanned");
	    	BigDecimal costExtrapolatedFather = (BigDecimal)get_Value("CostExtrapolated");	    	
	    	
	    	// BigDecimal revenuePlannedSons = (BigDecimal)get_Value("RevenuePlannedLL");
	    	BigDecimal revenueExtrapolatedSons =  (BigDecimal)get_Value("RevenueExtrapolatedLL");
	    	
	    	BigDecimal weightFather = (BigDecimal)get_Value("Weight");
	    	BigDecimal volumeFather = (BigDecimal)get_Value("Volume");	    	
	    	
			List<MProject> projectsOfFather = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
			.setParameters(getC_Project_ID())
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
					shareRevenue = revenueExtrapolatedSon.divide(revenueExtrapolatedSons, 5, BigDecimal.ROUND_HALF_DOWN);
				if (weight!=null && weightFather != null && weightFather.longValue()!=0)
					shareWeight = weight.divide(weightFather, 5, BigDecimal.ROUND_HALF_DOWN);
				if (volume!=null && volumeFather != null && volumeFather.longValue() != 0)
					shareVolume = volume.divide(volumeFather, 5, BigDecimal.ROUND_HALF_DOWN);
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
			set_ValueOfColumn("CostPlannedLL",         costPlannedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostAmtLL",             costAmtLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostNotInvoicedLL",     costNotInvoicedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostExtrapolatedLL",    costExtrapolatedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("RevenuePlannedLL",      revenuePlannedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("RevenueAmtLL",          revenueAmtLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("RevenueNotInvoicedLL",  revenueNotInvoicedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("RevenueExtrapolatedLL", revenueExtrapolatedLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostIssueProductLL",    costIssueProductLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostIssueResourceLL",   costIssueResourceLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostIssueInventoryLL",  costIssueInventoryLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostIssueSumLL",        costIssueSumLL.setScale(2, BigDecimal.ROUND_HALF_UP));
			set_ValueOfColumn("CostDiffExcecutionLL",  costDiffExcecutionLL.setScale(2, BigDecimal.ROUND_HALF_UP));

			// Gross margin LL = extrapolated revenue LL - (extrapolated costs LL + resource issue costs LL + inventory issue costs LL)
			grossMarginLL = revenueExtrapolatedLL.subtract(costExtrapolatedLL).
					subtract(costIssueResourceLL).subtract(costIssueInventoryLL);
			if(grossMarginLL==null)
				grossMarginLL = Env.ZERO;
			set_ValueOfColumn("GrossMarginLL",grossMarginLL.setScale(2, BigDecimal.ROUND_HALF_UP));		

			saveEx();  // Low Level Amounts
		}


		int C_Project_Parent_ID = get_ValueAsInt("C_Project_Parent_ID");  // Father Project -if any
		if (C_Project_Parent_ID!= 0)	{	
			// Project is child: update direct parent project
	    	MProject fatherProject = new MProject(getCtx(), C_Project_Parent_ID, get_TrxName());
			result = calcCostOrRevenuePlannedSons(C_Project_Parent_ID, false, true);      // Planned costs from Purchase Orders of all children of parent project
			fatherProject.set_Value("CostPlannedLL", result.setScale(2, BigDecimal.ROUND_HALF_UP));
			result = calcCostOrRevenueActualSons(C_Project_Parent_ID, false, true);       // Actual costs from Purchase Invoices of all children of parent project
			fatherProject.set_Value("CostAmtLL", result.setScale(2, BigDecimal.ROUND_HALF_UP));
			result = calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, false, true); // Sum of actual costs and planned (not yet invoiced) costs of all children of parent project
			fatherProject.set_Value("CostExtrapolatedLL", result.setScale(2, BigDecimal.ROUND_HALF_UP));		
			
			fatherProject.saveEx();

	    	BigDecimal costActualFather       = (BigDecimal)fatherProject.get_Value("CostAmt");
	    	BigDecimal costPlannedFather      = (BigDecimal)fatherProject.get_Value("CostPlanned");
	    	BigDecimal costExtrapolatedFather = (BigDecimal)fatherProject.get_Value("CostExtrapolated");

	    	BigDecimal revenueAmtSons         = calcCostOrRevenueActualSons(C_Project_Parent_ID, true, true);	
	    	BigDecimal revenuePlannedSons     = calcCostOrRevenuePlannedSons(C_Project_Parent_ID, true, true);
	    	BigDecimal revenueAllExtrapolated = calcCostOrRevenueExtrapolatedSons(C_Project_Parent_ID, true, true);	
	    	
	    	BigDecimal weightFather = (BigDecimal)fatherProject.get_Value("Weight");
	    	BigDecimal volumeFather = (BigDecimal)fatherProject.get_Value("Volume");
	    	
	    	
			List<MProject> projectsOfFather = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
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
					shareRevenue = revenueExtrapolatedSon.divide(revenueAllExtrapolated, 5, BigDecimal.ROUND_HALF_DOWN);
				if (weight!=null && weightFather != null && weightFather.longValue()!=0)
					shareWeight = weight.divide(weightFather, 5, BigDecimal.ROUND_HALF_DOWN);
				if (volume!=null && volumeFather != null && volumeFather.longValue()!= 0)
					shareVolume = volume.divide(volumeFather, 5, BigDecimal.ROUND_HALF_DOWN);
				calcCostPlannedInherited(sonProject, costPlannedFather,costActualFather,costExtrapolatedFather, shareVolume, shareWeight, shareRevenue);				
			}
			fatherProject.set_ValueOfColumn("RevenuePlannedLL", revenuePlannedSons.setScale(2, BigDecimal.ROUND_HALF_UP));
			fatherProject.set_ValueOfColumn("RevenueAmtLL", revenueAmtSons.setScale(2, BigDecimal.ROUND_HALF_UP));
			fatherProject.set_ValueOfColumn("RevenueExtrapolatedLL", revenueAllExtrapolated.setScale(2, BigDecimal.ROUND_HALF_UP));
			fatherProject.saveEx();
			saveEx();
		}

		BigDecimal grossMarginTotal = ((BigDecimal)get_Value("GrossMargin")).add(grossMarginLL);
		if(grossMarginTotal==null)
			grossMarginTotal = Env.ZERO;
		set_ValueOfColumn("GrossMarginTotal", grossMarginTotal.setScale(2, BigDecimal.ROUND_HALF_UP));
		
		Date date = new Date();
		long time = date.getTime();
		Timestamp timestamp = new Timestamp(time);
		set_ValueOfColumn("DateLastRun", timestamp);
		saveEx();
		
		return "";
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
    	result = new Query(getCtx(), MInvoiceLine.Table_Name, whereClause.toString(), get_TrxName())
    		.setParameters(c_Project_ID)
    		.aggregate(expresion, Query.AGGREGATE_SUM);
    	return result==null?Env.ZERO:result;
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
    	son.set_Value("CostPlannedInherited", result);
    	result = costPlannedFather.multiply(shareVolume);
    	son.set_Value("CostPlannedVolumeInherited", result);
    	result = costPlannedFather.multiply(shareWeight);
    	son.set_Value("CostPlannedWeightInherited", result);

    	result = costActualFather.multiply(shareRevenue);
    	son.set_Value("CostAmtInherited", result);
    	result = costActualFather.multiply(shareVolume);
    	son.set_Value("CostAmtVolumeInherited", result);
    	result = costActualFather.multiply(shareWeight);
    	son.set_Value("CostAmtWeightInherited", result);
    	
    	result = costExtrapolatedFather.multiply(shareRevenue);
    	son.set_Value("CostExtrapolatedInherited", result);
    	result = costExtrapolatedFather.multiply(shareVolume);
    	son.set_Value("CostExtrapolatedVolInherited", result);
    	result = costExtrapolatedFather.multiply(shareWeight);
    	son.set_Value("CostExtrapolatedWghtInherited", result);
    	
    	if (son.getC_Project_ID() != getC_Project_ID())
    		son.saveEx();    	
    	return true;
    }
    
    
    public String createDistribution()
    {     	  
    	if (isSummary() || get_ValueAsInt("C_Project_Parent_ID") == 0)
    		return "";
    		
        for (MAcctSchema as:MAcctSchema.getClientAcctSchema(getCtx(), getAD_Client_ID()))
    		{
    			ArrayList<Object> params = new ArrayList<Object>();
    			params.add(get_ValueAsInt("C_Project_Parent_ID"));
    			params.add(as.getC_AcctSchema_ID());
    			List<MDistribution> distributions = new Query(getCtx(), MDistribution.Table_Name, "C_Project_ID =? " +
    					" and c_acctschema_ID=?", get_TrxName())
    			.setParameters(params)
    			.list();
    			for (MDistribution distribution:distributions)
    				distribution.delete(true);
    			createDistribution(as);
    			distributeOrders(as);
    			distributeInvoices(as);
    		}
    	return "";
    }
    
    private void createDistribution(MAcctSchema as)
    {
    	String ProjectDistribution = get_ValueAsString("ProjectDistribution");
    	if (ProjectDistribution == null || ProjectDistribution == "")
    		ProjectDistribution = "I";
    	MDistribution distribution = new MDistribution(getCtx()	, 0, get_TrxName());
    	distribution.setAnyProject(false);
    	distribution.setC_Project_ID(get_ValueAsInt("C_Project_Parent_ID"));
    	distribution.setName(getName());
    	distribution.setC_AcctSchema_ID(as.getC_AcctSchema_ID());
    	distribution.setIsCreateReversal(false);
    	distribution.saveEx();
    	MProject father = new MProject(getCtx(), get_ValueAsInt("C_Project_Parent_ID"), get_TrxName());
    	List<MProject> projectsOfFather = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
    	.setParameters(get_ValueAsInt("C_Project_Parent_ID"))
    	.list();
    	BigDecimal weight_father = (BigDecimal)father.get_Value("Weight");
    	BigDecimal Volume_father = (BigDecimal)father.get_Value("Volume");
    	BigDecimal share = Env.ONEHUNDRED;
    	BigDecimal revenueAll = calcCostOrRevenueActualSons(get_ValueAsInt("C_Project_ID"), true, true);	
    	
    		for (MProject projectson: projectsOfFather)
    		{
    			if (projectsOfFather.size() > 1)
    			{
    				BigDecimal weight = (BigDecimal)projectson.get_Value("Weight");
    				BigDecimal volume = (BigDecimal)projectson.get_Value("volume");
    				if (weight.compareTo(Env.ZERO) == 0 && volume.compareTo(Env.ZERO)==0
    						&& revenueAll.compareTo(Env.ZERO)!= 0 
    						&& projectson.calcCostOrRevenueActual(projectson.getC_Project_ID(), true, false).compareTo(Env.ZERO)!=0)
    				{
    					share = projectson.calcCostOrRevenueActual(projectson.getC_Project_ID(), true, false).divide(revenueAll,  5, BigDecimal.ROUND_HALF_DOWN);
    				}
    				else
    				{
    					if (ProjectDistribution.equals("W") && weight_father.compareTo(Env.ZERO)==0)
    						return ;

    					if (ProjectDistribution.equals("V") && Volume_father.compareTo(Env.ZERO)==0)
    						return ;
    					share = ProjectDistribution.equals("W")?
    							weight.divide(weight_father, 5, BigDecimal.ROUND_HALF_DOWN)
    							: volume.divide(Volume_father, 5, BigDecimal.ROUND_HALF_DOWN);
    				}
    			}
				share = share.multiply(Env.ONEHUNDRED); 
    			MDistributionLine dLine = new MDistributionLine(getCtx(), 0, get_TrxName());
    			dLine.setGL_Distribution_ID(distribution.getGL_Distribution_ID());
    			dLine.setOverwriteProject(true);
    			dLine.setC_Project_ID(projectson.getC_Project_ID());
    			dLine.setPercent(share);
    			distribution.setPercentTotal(distribution.getPercentTotal().add(share));
    			dLine.saveEx();			
    		}
    	distribution.saveEx();
    	BigDecimal diff = Env.ONEHUNDRED.subtract(distribution.getPercentTotal());
    	if (diff.compareTo(Env.ZERO)!=0)
    	{
    		final String whereClause = I_GL_DistributionLine.COLUMNNAME_GL_Distribution_ID+"=?";
    		MDistributionLine maxLine = new Query(getCtx(),I_GL_DistributionLine.Table_Name,whereClause,get_TrxName())
    		.setParameters(distribution.getGL_Distribution_ID())
    		.setOrderBy(MDistributionLine.COLUMNNAME_Percent + " desc")
    		.first();
    		maxLine.setPercent(maxLine.getPercent().add(diff));
    		maxLine.saveEx();
    	}
    	distribution.validate();
    }
    
    private String distributeOrders(MAcctSchema as)
	{
    	ArrayList<MOrder> ordersToPost = new ArrayList<MOrder>();
		String whereClause = "C_Project_ID=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(get_ValueAsInt("C_Project_Parent_ID"));

		List<MOrderLine> oLines = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
		.setParameters(params)
		.setOrderBy("C_Order_ID ")
		.list();
		if (oLines == null)
			return"";
		for (MOrderLine oLine:oLines)
		{
			if ((oLine.getC_Order().getDocStatus().equals(MOrder.DOCSTATUS_Drafted)
					|| oLine.getC_Order().getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
					|| oLine.getC_Order().getDocStatus().equals(MOrder.DOCSTATUS_Invalid)))
				continue;
			//if (MPeriod.isOpen(getCtx(), oLine.getC_Order().getDateAcct()
			//		, oLine.getC_Order().getC_DocType().getDocBaseType(), oLine.getAD_Org_ID()))
			{

				Boolean isadded = false;
				for (MOrder order:ordersToPost)
				{
					if (order.getC_Order_ID() ==  oLine.getC_Order_ID())
					{
						isadded = true;
						break;
					}
				}
				if (!isadded)
					ordersToPost.add(oLine.getParent());
			}
			//else
			{
				//createJournal(as, oLine);						
			}
		}
		for (MOrder order:ordersToPost)
		{
			clearAccounting(as, order);}
		return "";
	}
	

	private String distributeInvoices(MAcctSchema as)
	{
		String whereClause = "C_Project_ID=?";
		ArrayList<MInvoice> invoicesToPost = new ArrayList<MInvoice>();
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(getC_Project_ID());
		List<MInvoiceLine> oLines = new Query(getCtx(), MInvoiceLine.Table_Name, whereClause, get_TrxName())
		.setParameters(params)
		.setOrderBy("C_Invoice_ID ")
		.list();
		if (oLines == null)
			return"";
		for (MInvoiceLine oLine:oLines)
		{
			if ((oLine.getC_Invoice().getDocStatus().equals(MOrder.DOCSTATUS_Drafted)
					|| oLine.getC_Invoice().getDocStatus().equals(MOrder.DOCSTATUS_InProgress)
					|| oLine.getC_Invoice().getDocStatus().equals(MOrder.DOCSTATUS_Invalid)))
				continue;
			if (MPeriod.isOpen(getCtx(), oLine.getC_Invoice().getDateAcct()
					, oLine.getC_Invoice().getC_DocType().getDocBaseType(), oLine.getAD_Org_ID()))
			{

				Boolean isadded = false;
				for (MInvoice invoice:invoicesToPost)
				{
					if (invoice.getC_Invoice_ID() ==  oLine.getC_Invoice_ID())
					{
						isadded = true;
						break;
					}
				}
				if (!isadded)
					invoicesToPost.add(oLine.getParent());
			}
			else
			{
				//createJournal(as, oLine);						
			}
			for (MInvoice invoice:invoicesToPost)
			{
				clearAccounting(as, invoice);
			}
		}
		return "";
	
	}
	
	public boolean clearAccounting(MAcctSchema accountSchema, PO model) 
	{
		final String sqlUpdate = "UPDATE " + model.get_TableName() + " SET Posted = 'N' WHERE "+ model.get_TableName() + "_ID=?";
		int no = DB.executeUpdate(sqlUpdate, new Object[] {model.get_ID()}, false , model.get_TrxName());
		//Delete account
		final String sqldelete = "DELETE FROM Fact_Acct WHERE Record_ID =? AND AD_Table_ID=?";		
		no = DB.executeUpdate (sqldelete ,new Object[] { model.get_ID(),
				model.get_Table_ID() }, false , model.get_TrxName());
		return true;
	}	
	
	public String updateWeightVolume()
	{

        if (!(getProjectCategory().equals("M")
                || getProjectCategory().equals("T")))
            return "";
        
        if (getProjectCategory().equals("T"))
        {

        	int C_Project_Parent_ID = get_ValueAsInt("C_Project_Parent_ID");
        	if (C_Project_Parent_ID == 0)
        		return "" ;
            MProject parent = new MProject(getCtx(), get_ValueAsInt("C_Project_Parent_ID"), get_TrxName());
            BigDecimal total = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
                .setParameters(get_ValueAsInt("C_Project_Parent_ID"))
                .aggregate("Weight", Query.AGGREGATE_SUM);
            parent.set_ValueOfColumn("Weight", total);
            total = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
                .setParameters(get_ValueAsInt("C_Project_Parent_ID"))
                .aggregate("Volume", Query.AGGREGATE_SUM);
            parent.set_ValueOfColumn("Volume", total);
            parent.saveEx();
            return "";
        }
        MProject parent = new MProject(getCtx(),get_ValueAsInt("C_Project_ID"), get_TrxName());
        List<MProject> sons = new Query(getCtx(), MProject.Table_Name, "C_Project_Parent_ID=?", get_TrxName())
        .setParameters(get_ValueAsInt("C_Project_ID"))
        .list();        
        BigDecimal totalWeight = Env.ZERO;
        BigDecimal totalVolume = Env.ZERO;
        for (MProject son:sons)
        {
            BigDecimal rateWeight = MUOMConversion.convert(son.get_ValueAsInt("C_UOM_ID"), 
                    parent.get_ValueAsInt("C_UOM_ID"), (BigDecimal)son.get_Value("WeightEntered"), true);

            BigDecimal rateVolume = MUOMConversion.convert(son.get_ValueAsInt("C_UOM_Volume_ID"), 
                    parent.get_ValueAsInt("C_UOM_Volume_ID"), (BigDecimal)son.get_Value("VolumeEntered"), true);
            son.set_ValueOfColumn("Weight", rateWeight);
            son.set_ValueOfColumn("Volume", rateVolume);
            totalWeight = totalWeight.add(rateWeight);
            totalVolume = totalVolume.add(rateVolume);
            son.saveEx();
        }
        parent.set_ValueOfColumn("Weight", totalWeight);
        parent.set_ValueOfColumn("Volume", totalVolume);
        parent.saveEx();
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
		params.add(getC_Project_ID());
		
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
		params.add(getC_Project_ID());
		
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

		List<MProject> childrenProjects = new Query(getCtx(), MProject.Table_Name, whereClause, get_TrxName())
		.setParameters(params)
		.list();

		MProject project = new MProject(getCtx(), c_Project_ID, get_TrxName());
		if (childrenProjects == null) {	
			// No children -> just update this project
			project.updateProjectPerformanceCalculation();
			return"";	
		}
		
		for (MProject childProject:childrenProjects) {
			// update all children of this child
			updateProjectPerformanceCalculationSons(childProject.getC_Project_ID(), levelCount+1);
		}
		// last but not least, update this project
		project.updateProjectPerformanceCalculation();
		return"";
    }
	
}	//	MProject
