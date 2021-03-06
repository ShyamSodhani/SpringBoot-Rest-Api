package com.example.TestingKeycloak.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
//import com.keycloak.sample.dto.UserDTO;
import com.example.TestingKeycloak.dto.UserDTO;






@RequestMapping(value = "/users")
@RestController
@CrossOrigin
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private String authServerUrl = "https://keycloakserver3.azurewebsites.net/auth";
    private String realm = "Testing";
    private String clientId = "nodejs-microservice";
    private String role = "user";
    //Get client secret from the Keycloak admin console (in the credential tab)
    private String clientSecret = "5b37a114-eab1-4c15-9c67-c821b22b85ce";

    @PostMapping(path = "/create")
    public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {

        Keycloak keycloak = KeycloakBuilder.builder().serverUrl(authServerUrl)
                .grantType(OAuth2Constants.PASSWORD).realm("master").clientId("admin-cli")
                .username("admin").password("admin")
                .resteasyClient(new ResteasyClientBuilder().connectionPoolSize(10).build()).build();

        keycloak.tokenManager().getAccessToken();


        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(userDTO.getEmail());
        user.setFirstName(userDTO.getFirstname());
        user.setLastName(userDTO.getLastname());
        user.setEmail(userDTO.getEmail());

        // Get realm
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource usersRessource = realmResource.users();

        Response response = usersRessource.create(user);

        userDTO.setStatusCode(response.getStatus());
        userDTO.setStatus(response.getStatusInfo().toString());

        if (response.getStatus() == 201) {

            String userId = CreatedResponseUtil.getCreatedId(response);

            log.info("Created userId {}", userId);


            // create password credential
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(userDTO.getPassword());

            UserResource userResource = usersRessource.get(userId);

            // Set password credential
            userResource.resetPassword(passwordCred);

            // Get realm role student
            RoleRepresentation realmRoleUser = realmResource.roles().get(role).toRepresentation();

            // Assign realm role student to user
            userResource.roles().realmLevel().add(Arrays.asList(realmRoleUser));
        }
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping(path = "/signin")
    public ResponseEntity<?> signin(@RequestBody UserDTO userDTO) {

        Map<String, Object> clientCredentials = new HashMap<>();
        clientCredentials.put("secret", clientSecret);
        clientCredentials.put("grant_type", "password");

        Configuration configuration =
                new Configuration(authServerUrl, realm, clientId, clientCredentials, null);
        AuthzClient authzClient = AuthzClient.create(configuration);

        AccessTokenResponse response =
                authzClient.obtainAccessToken(userDTO.getEmail(), userDTO.getPassword());

        return ResponseEntity.ok(response);
    }


//    @GetMapping(value = "/unprotected-data")
//    public String getName() {
//        return "Hello, this api is not protected.";
//    }
//
//    @RolesAllowed("admin")
//    @GetMapping(value = "/protected-data")
//    public String getEmail() {
//        return "Hello, this api is protected.";
//    }
    
    @RequestMapping(value = "/anonymous", method = RequestMethod.GET)
    public ResponseEntity<String> getAnonymous() {
        return ResponseEntity.ok("Hello Anonymous");
    }


    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public ResponseEntity<String> getUser(@RequestHeader String Authorization) {
        return ResponseEntity.ok("Hello User");
    }


    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public ResponseEntity<String> getAdmin(@RequestHeader String Authorization) {
        return ResponseEntity.ok("Hello Admin");
    }


    @RequestMapping(value = "/all-user", method = RequestMethod.GET)
    public ResponseEntity<String> getAllUser(@RequestHeader String Authorization) {
        return ResponseEntity.ok("Hello All User");
    }
    
    @GetMapping(path="/hello")
    public String hello() 
    {
        return "Hello without authentication";
    }
    


}
