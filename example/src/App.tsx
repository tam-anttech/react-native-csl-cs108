import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import { Cs108Manager } from 'react-native-csl-cs108';

let bleManger;

export default function App() {
  React.useEffect(() => {
    bleManger = new Cs108Manager();
    console.log(bleManger.getId());
  }, []);

  return (
    <View style={styles.container}>
      <View style={styles.wrapText}>
        <Text style={styles.text}>_</Text>
      </View>
      <View style={styles.tools}>
        <View style={styles.button}>
          <Text style={styles.buttonText}>Scan</Text>
        </View>
        <View style={styles.button}>
          <Text style={styles.buttonText}>Connect</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#202020',
  },
  wrapText: {
    flex: 1,
    width: '100%',
    padding: 5,
  },
  text: {
    flex: 1,
    width: '100%',
    fontSize: 14,
    color: '#eee',
  },
  tools: {
    height: 70,
    width: '100%',
    backgroundColor: '#000',
    flexDirection: 'row',
    paddingVertical: 10,
  },
  button: {
    height: '100%',
    flex: 1,
    marginHorizontal: 5,
    borderRadius: 5,
    padding: 5,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#808080',
  },
  buttonText: {
    fontSize: 16,
    color: '#eee',
  },
});
