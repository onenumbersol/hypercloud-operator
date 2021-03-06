package k8s.example.client.models;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

public class RegistryStatus {
	private List<RegistryCondition> conditions = null;
	private String phase = null;
	private String message = null;
	private String reason = null;
	private DateTime phaseChangedAt = null;
	private String capacity = null;

	public List<RegistryCondition> getConditions() {
		return conditions;
	}

	public void setConditions(List<RegistryCondition> conditions) {
		this.conditions = conditions;
	}

	public RegistryStatus conditions(List<RegistryCondition> conditions) {
		this.conditions = conditions;
		return this;
	}

	public RegistryStatus addConditionsItem(RegistryCondition conditionsItem) {
		if (this.conditions == null) {
			this.conditions = new ArrayList<RegistryCondition>();
		}
		this.conditions.add(conditionsItem);
		return this;
	}

	public String getPhase() {
		return phase;
	}

	public void setPhase(String phase) {
		this.phase = phase;
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public DateTime getPhaseChangedAt() {
		return phaseChangedAt;
	}

	public void setPhaseChangedAt(DateTime phaseChangedAt) {
		this.phaseChangedAt = phaseChangedAt;
	}

	public String getCapacity() {
		return capacity;
	}

	public void setCapacity(String capacity) {
		this.capacity = capacity;
	}

	public static enum StatusPhase {
		CREATING("Creating"),
		UPDATING("Updating"),
		RUNNING("Running"),
		NOT_READY("NotReady"),
		ERROR("Error"),
		;
		
		private String status;

		StatusPhase(String status) { this.status = status; }
		public String getStatus() {	return status; }
	}
	
	public static enum Status {
		TRUE("True"),
		FALSE("False"),
		UNUSED_FIELD("UnusedField"),
		;
		
		private String status;

		Status(String status) { this.status = status; }
		public String getStatus() {	return status; }
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class RegistryStatus {\n");
		if(conditions != null ) 
			for( RegistryCondition condition : conditions)
				sb.append("    conditions: ").append(toIndentedString(condition)).append("\n");
		sb.append("    message: ").append(toIndentedString(message)).append("\n");
		sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
		sb.append("    phase: ").append(toIndentedString(phase)).append("\n");
		sb.append("    phaseChangedAt: ").append(toIndentedString(phaseChangedAt)).append("\n");
		sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {
		if (o == null) {
			return "null";
		}
		return o.toString().replace("\n", "\n    ");
	}

}
