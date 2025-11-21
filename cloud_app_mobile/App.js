import { StyleSheet, View } from "react-native";
import GoogleLogin from "./components/GoogleLogin";

export default function App() {
  return (
    <View style={styles.container}>
      <GoogleLogin />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
});
