import * as AuthSession from "expo-auth-session";
import * as WebBrowser from "expo-web-browser";
import React from "react";
import { Button } from "react-native";

WebBrowser.maybeCompleteAuthSession();

const CLIENT_ID = "YOUR_ANDROID_OR_IOS_CLIENT_ID_HERE";
const REDIRECT_URI = AuthSession.makeRedirectUri({
  scheme: "com.bbg", // match the one registered in Google Cloud
});

const GoogleLogin = () => {
  const [request, response, promptAsync] = AuthSession.useAuthRequest(
    {
      clientId: CLIENT_ID,
      scopes: ["https://www.googleapis.com/auth/drive.file", "profile", "email"],
      redirectUri: REDIRECT_URI,
    },
    { authorizationEndpoint: "https://accounts.google.com/o/oauth2/v2/auth" }
  );

  React.useEffect(() => {
    if (response?.type === "success") {
      const code = response.params.code;
      console.log("Google auth code:", code);

      // Send code to FastAPI backend
      fetch("https://YOUR-NGROK-URL/api/google/mobile_callback", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      })
        .then((res) => res.json())
        .then((data) => console.log("Backend response:", data))
        .catch(console.error);
    }
  }, [response]);

  return (
    <Button
      disabled={!request}
      title="Login with Google Drive"
      onPress={() => promptAsync()}
    />
  );
};

export default GoogleLogin;
