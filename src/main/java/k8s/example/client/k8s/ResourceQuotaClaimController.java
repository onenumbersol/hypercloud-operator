package k8s.example.client.k8s;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.util.Watch;
import k8s.example.client.Constants;
import k8s.example.client.models.NamespaceClaim;

public class ResourceQuotaClaimController extends Thread {
	private final Watch<NamespaceClaim> rqcController;
	private static int latestResourceVersion = 0;
	private CustomObjectsApi api = null;

	ResourceQuotaClaimController(ApiClient client, CustomObjectsApi api, int resourceVersion) throws Exception {
		rqcController = Watch.createWatch(client,
				api.listClusterCustomObjectCall("tmax.io", "v1", Constants.CUSTOM_OBJECT_PLURAL_RESOURCEQUOTACLAIM, null, null, null, null, null, Integer.toString( resourceVersion ), null, Boolean.TRUE, null),
				new TypeToken<Watch.Response<NamespaceClaim>>() {}.getType());
		this.api = api;
		latestResourceVersion = resourceVersion;
	}
	
	@Override
	public void run() {
		try {
			rqcController.forEach(response -> {
				try {
					if (Thread.interrupted()) {
						System.out.println("Interrupted!");
						rqcController.close();
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				
				
				// Logic here
				String claimName = "unknown";
				String claimNamespace = "unknown";
				try {
					NamespaceClaim claim = response.object;

					if( claim != null) {
						latestResourceVersion = Integer.parseInt( response.object.getMetadata().getResourceVersion() );
						String eventType = response.type.toString(); //ADDED, MODIFIED, DELETED
						System.out.println("[ResourceQuotaClaim Controller] Event Type : " + eventType );
						System.out.println("[ResourceQuotaClaim Controller] == ResourceQuotaClaim == \n" + claim.toString());
						claimName = claim.getMetadata().getName();
						claimNamespace = claim.getMetadata().getNamespace();
						switch( eventType ) {
							case Constants.EVENT_TYPE_ADDED : 
								// Patch Status to Awaiting
								replaceRqcStatus( claim.getMetadata().getName(), Constants.CLAIM_STATUS_AWAITING, "wait for admin permission", claimNamespace );
								break;
							case Constants.EVENT_TYPE_MODIFIED : 
								String status = getClaimStatus( claim.getMetadata().getName(), claimNamespace );
								if ( status.equals( Constants.CLAIM_STATUS_SUCCESS ) && K8sApiCaller.resourcequotaAlreadyExist( claimNamespace ) ) {
									K8sApiCaller.updateResourceQuota( claim );
									replaceRqcStatus( claim.getMetadata().getName(), Constants.CLAIM_STATUS_SUCCESS, "resource quota update success.", claimNamespace );
								} else if ( status.equals( Constants.CLAIM_STATUS_SUCCESS ) && !K8sApiCaller.resourcequotaAlreadyExist( claimNamespace ) ) {
									K8sApiCaller.createResourceQuota( claim );
									replaceRqcStatus( claim.getMetadata().getName(), Constants.CLAIM_STATUS_SUCCESS, "resource quota create success.", claimNamespace );
								}
								break;
							case Constants.EVENT_TYPE_DELETED : 
								// Nothing to do
								break;
						}
					}
					
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					System.out.println(sw.toString());
					try {
						replaceRqcStatus( claimName, Constants.CLAIM_STATUS_ERROR, e.getMessage(), claimNamespace );
					} catch (ApiException e1) {
						e1.printStackTrace();
						System.out.println("Resource Quota Claim Controller Exception : Change Status 'Error' Fail ");
					}
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		} catch (Exception e) {
			System.out.println("Resource Quota Claim Controller Exception: " + e.getMessage());
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}

	@SuppressWarnings("unchecked")
	private void replaceRqcStatus( String name, String status, String reason, String namespace ) throws ApiException {
		JsonArray patchStatusArray = new JsonArray();
		JsonObject patchStatus = new JsonObject();
		JsonObject statusObject = new JsonObject();
		patchStatus.addProperty("op", "replace");
		patchStatus.addProperty("path", "/status");
		statusObject.addProperty( "status", status );
		statusObject.addProperty( "reason", reason );
		patchStatus.add("value", statusObject);
		patchStatusArray.add( patchStatus );
		
		System.out.println( "Patch Status Object : " + patchStatusArray );
		/*[
		  "op" : "replace",
		  "path" : "/status",
		  "value" : {
		    "status" : "Awaiting"
		  }
		]*/
		try {
			api.patchNamespacedCustomObjectStatus(
					Constants.CUSTOM_OBJECT_GROUP, 
					Constants.CUSTOM_OBJECT_VERSION, 
					namespace, 
					Constants.CUSTOM_OBJECT_PLURAL_RESOURCEQUOTACLAIM,  
					name, 
					patchStatusArray );
		} catch (ApiException e) {
			System.out.println(e.getResponseBody());
			System.out.println("ApiException Code: " + e.getCode());
			throw e;
		}
	}
	
	@SuppressWarnings("unchecked")
	private String getClaimStatus( String name, String namespace ) throws ApiException {
		Object claimJson = null;
		try {
			claimJson = api.getNamespacedCustomObject(
					Constants.CUSTOM_OBJECT_GROUP, 
					Constants.CUSTOM_OBJECT_VERSION, 
					namespace, 
					Constants.CUSTOM_OBJECT_PLURAL_RESOURCEQUOTACLAIM,  
					name );
		} catch (ApiException e) {
			System.out.println(e.getResponseBody());
			System.out.println("ApiException Code: " + e.getCode());
			throw e;
		}

		String objectStr = new Gson().toJson( claimJson );
		System.out.println( objectStr );
		
		JsonParser parser = new JsonParser();
		String status = parser.parse( objectStr ).getAsJsonObject().get( "status" ).getAsJsonObject().get( "status" ).getAsString();
		
		System.out.println( "Status : " + status );

		return status;
	}
	
	public static int getLatestResourceVersion() {
		return latestResourceVersion;
	}
}