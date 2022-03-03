CREATE OR REPLACE FUNCTION linenetamtreturned(p_invoiceline_id numeric)
  RETURNS numeric AS
$BODY$

DECLARE
	v_returnedlinenetamtreal numeric;
BEGIN
	v_returnedlinenetamtreal= 0;
	
	IF (p_invoiceline_id = 0) THEN
		RETURN v_reversallinenetamtreal;
	END IF;
	
	SELECT	COALESCE ( linenetamtrealinvoiceline(crnl.C_Invoiceline_ID), 0)
        INTO	v_returnedlinenetamtreal
   	FROM	   C_Invoiceline ivl
	INNER JOIN M_InOutLine iol    ON (ivl.M_InOutLine_ID=iol.M_InOutLine_ID)
	INNER JOIN M_RMALine rmal     ON (iol.M_InOutLine_ID=rmal.M_InOutLine_ID)
	INNER JOIN M_InOutLine retl   ON (rmal.M_RMALine_ID=retl.M_RMALine_ID)
	INNER JOIN C_Invoiceline crnl ON (retl.M_InOutLine_ID=crnl.M_InOutLine_ID)
	INNER JOIN C_Invoice crn      ON (crnl.C_Invoice_ID=crn.C_Invoice_ID AND crn.docstatus='CO')
	WHERE ivl.C_Invoiceline_ID=p_invoiceline_id;;
	
	RETURN coalesce(v_returnedlinenetamtreal, 0);
END;

$BODY$
  LANGUAGE plpgsql;
