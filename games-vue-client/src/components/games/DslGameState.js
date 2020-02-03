import Vue from "vue";

import Socket from "../../socket";

const gameStore = {
  namespaced: true,
  state: {
    games: {}
  },
  getters: {},
  mutations: {
    createGame(state, data) {
      Vue.set(state.games, data.gameId, {
        gameInfo: {
          gameType: data.gameType,
          gameId: data.gameId,
          yourIndex: data.yourIndex,
          players: data.players
        },
        gameData: {
          view: {}
        }
      });
    },
    updateView(state, data) {
      let game = state.games[data.gameId].gameData;
      game.view = data.view
    }
  },
  actions: {
    onSocketMessage(context, data) {
      if (data.type === "GameStarted") {
        context.commit("createGame", data);
      }
      if (data.type === "GameView") {
        context.commit("updateView", data);
      }
      if (data.type === "GameMove") {
        Socket.send(`{ "type": "ViewRequest", "gameType": "${data.gameType}", "gameId": "${data.gameId}" }`);
      }
    }
  }
};

export default gameStore;
