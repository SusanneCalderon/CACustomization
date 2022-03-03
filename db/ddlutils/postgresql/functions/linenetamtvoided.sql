CREATE OR REPLACE FUNCTION linenetamtvoided(p_invoicereversalline_id numeric)
  RETURNS numeric AS
$BODY$

DECLARE
	v_reversallinenetamt	numeric;
	v_reversallinenetamtreal numeric;
BEGIN
	v_reversallinenetamt    = 0;
	v_reversallinenetamtreal= 0;
	
	IF (p_invoicereversalline_id = 0) THEN
		RETURN v_reversallinenetamtreal;
	END IF;
	
	SELECT	COALESCE (linenetamt, 0)
        INTO	v_reversallinenetamt
   	FROM	C_Invoiceline
	WHERE C_Invoiceline_ID=p_invoicereversalline_id;
	
	IF (v_reversallinenetamt = 0) THEN
		RETURN v_reversallinenetamtreal;
	END IF;

	v_reversallinenetamtreal = linenetamtrealinvoiceline(p_invoicereversalline_id);
	RETURN  coalesce(v_reversallinenetamtreal, 0);
END;

$BODY$
  LANGUAGE plpgsql;
