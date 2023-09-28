import Path from "path";
import NodeProcess from "@src/process/processes/NodeProcess";
import WebProcess from "@src/process/processes/WebProcess";
import {addInputProgram, InputProgram} from "./InputProgram";

/**
 * To Add a Program:
 * Step 1) Extend enum `BenchmarksEnum`
 * Step 2) add a new call to `addInputProgram` below
 */

/**
 * Enum containing appl names
 */
export enum BenchmarksEnum {
    FS_CALCULATOR        = "FS_CALCULATOR",
    FS_3BUTTONS          = "FS_3BUTTONS",
    FS_2BUTTONS          = "FS_2BUTTONS",
    FS_WHITEBOARD        = "FS_WHITEBOARD",
    FS_JQUERY_CHAT       = "FS_JQUERY_CHAT",
    FS_FORM              = "FS_FORM",
    FS_SIMPLE_CHAT       = "FS_SIMPLE_CHAT",
    FS_CHAT              = "FS_CHAT",
    FS_CARD_FU           = "FS_CARD_FU",
    FS_TIERLESS          = "FS_TIERLESS",
    FS_CODEMON           = "FS_CODEMON",
    FS_LIFE              = "FS_LIFE",

    HTTP_SESSION         = "HTTP_SESSION",

    FS_INSECURE_CHAT     = "FS_INSECURE_CHAT",
    FS_TOTEMS            = "FS_TOTEMS",
    FS_TOHACKS           = "FS_TOHACKS",

    FS_SAVINA_BIG        = "FS_SAVINA_BIG",
    FS_BANK_TRANSFER     = "FS_BANK_TRANSFER",
    FS_RNG_SEEDED_IMPOSSIBLE = "FS_RNG_SEEDED_IMPOSSIBLE",
    OBJECTSTEST              = "OBJECTSTEST",
    RUBENTEST                = "RUBENTEST",

    MAIN                 = "MAIN",
    NEW_EVENTS           = "FS_NEW_EVENTS",
    STRINGS              = "FS_STRINGS",
    FS_VERIFY_INTRA      = "FS_VERIFY_INTRA",
    IV_CALCULATOR        = "IV_CALCULATOR",
    SUMMARIES            = "SUMMARIES",
    SUMMARIES2           = "SUMMARIES2",
    SUMMARIES3           = "SUMMARIES3",
    SUMMARIES4           = "SUMMARIES4",
    SUMMARIES5           = "SUMMARIES5",
    SUMMARIES6           = "SUMMARIES6",
    SUMMARIES7           = "SUMMARIES7",

    NAMES_1              = "NAMES_1",
    NAMES_2              = "NAMES_2",

    // Programs to test for Thesis Francis
    THESIS_BANK_TRANSFER   =  "THESIS_BANK_TRANSFER",

    // Programs to be used as fixtures for testing
    FIXT_MAIN            = "FIXT_MAIN",
    FIXT_FS_WHITEBOARD   = "FIXT_FS_WHITEBOARD",
    FIXT_FS_VERIFY_INTRA = "FIXT_FS_VERIFY_INTRA",
    FIXT_NAMES_1         = "FIXT_NAMES_1",
    FIXT_NAMES_2         = "FIXT_NAMES_2",
    FIXT_2BUTTONS        = "FIXT_2BUTTONS",
    FS_NEW_EVENTS        = "FS_NEW_EVENTS",
    FS_STRINGS           = "FS_STRINGS",

    // State merging experiments
    MERGING1             = "MERGING1",
    MERGING2             = "MERGING2",
    MERGING3             = "MERGING3",
    MERGING4             = "MERGING4",
    MERGING5             = "MERGING5",
    MERGING6             = "MERGING6",
    MERGING7             = "MERGING7",
    MERGING_EVENTS1      = "MERGING_EVENTS1",
    MERGING_SCOPING1     = "MERGING_SCOPING1",
    MERGING_SCOPING2     = "MERGING_SCOPING2",
    MERGING_SCOPING3     = "MERGING_SCOPING3",
    MERGING_SCOPING4     = "MERGING_SCOPING4",
    MERGING_SCOPING5     = "MERGING_SCOPING5",
    MERGING_SCOPING6     = "MERGING_SCOPING6",
    MERGING_SCOPING7     = "MERGING_SCOPING7",
    MERGING_SCOPING8     = "MERGING_SCOPING8",
    MERGING_SCOPING9     = "MERGING_SCOPING9",
    MERGING_SCOPING10    = "MERGING_SCOPING10",
    MERGING_SCOPING11    = "MERGING_SCOPING11",
    MERGING_SCOPING12    = "MERGING_SCOPING12",
    MERGING_SCOPING13    = "MERGING_SCOPING13",

    REQUESTED            = "REQUESTED",
}

const inputProgramsFolderPath = Path.join(__dirname, "../../../../../input_programs")

export function addPrograms() {
    // Calculator: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_CALCULATOR, 
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/Calculator_sockets/index"), 0, 0),
                new WebProcess("calc_1", 1, 1, Path.join(inputProgramsFolderPath, "/Calculator_sockets/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    );

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_3BUTTONS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/3/index"), 0, 0),
                    new WebProcess("client_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // 2:
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_2BUTTONS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/2/index"), 0, 0),
                new WebProcess("client_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // RNG: FS_RNG_SEEDED_IMPOSSIBLE
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_RNG_SEEDED_IMPOSSIBLE,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/rng_seeded_impossible/index"), 0, 0),
                new WebProcess("client_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Whiteboard: Realistic, works
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_WHITEBOARD,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/whiteboard/index"), 0, 0),
            new WebProcess("whiteboard_1", 1, 1, Path.join(inputProgramsFolderPath, "/whiteboard/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // jQuery Chat: Realistic, works
        addInputProgram(
            new InputProgram(
                BenchmarksEnum.FS_JQUERY_CHAT,
                [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/socket.io/examples/chat/index"), 0, 0),
                new WebProcess("chat_1", 1, 1, Path.join(inputProgramsFolderPath, "/socket.io/examples/chat/index.js"), "http://localhost:3000/index.html", ["main.js"])]
            )
        )
    // new WebProcess("chat_2", 2, 1, Path.join(__dirname, "/../../../input_programs/socket.io/examples/chat/index_2.js"), ["main.js"])];
    //                                       new WebProcess("chat_3", 3, 1, Path.join(__dirname, "/../../../input_programs/socket.io/examples/chat/index_2.js"), "http://localhost:3000/index.html", ["main.js"])];

    // Form: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_FORM,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/socket.io/examples/form/index"), 0, 0),
            new WebProcess("form_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Simple chat: Realistic, but VERY simplistic, works
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_SIMPLE_CHAT,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/simple_chat/index"), 0, 0),
            new WebProcess("chat_1", 1, 1, Path.join(inputProgramsFolderPath, "/simple_chat/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )
    //                                       new WebProcess("chat_2", 2, 1, Path.join(__dirname, "/../../../input_programs/simple_chat/index_2.js"), "http://localhost:3000/index.html", ["main.js"]))];

    // Chat app: Realistic, works
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_CHAT,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/socket.io/examples/chat-app/app"), 0, 0),
            new WebProcess("chat_1", 1, 1, Path.join(inputProgramsFolderPath, "/socket.io/examples/chat-app/app.js"), "http://localhost:3000/index.html", ["javascripts/main.js"])]
        )
    )
    // new WebProcess("chat_2", 2, 1, Path.join(__dirname, "/../../../input_programs/socket.io/examples/chat-app/app_2.js"), "http://localhost:3000/index.html", ["javascripts/main.js"])];

    // Card-fu: Realistic, does not work
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_CARD_FU,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/card-fu/index"), 0, 0),
            new WebProcess("card_1", 1, 1, undefined, "http://localhost:3000/index.html", ["javascripts/game_client.js", "javascripts/konami.js"])]
        )
    )
    // new WebProcess("card_2", 2, 1, undefined, "http://localhost:3000/index.html", ["javascripts/game_client.js", "javascripts/konami.js"])];

    // Tierless - Unicorn: Realistic, does not work
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_TIERLESS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/stip/server_env/server"), 0, 0),
            new WebProcess("unicorn_1", 1, 1, undefined, "http://localhost:3000/index.html", [])]
        )
    )

    // Codemon: Realistic, almost works
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_CODEMON,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/Codemon/index"), 0, 0),
            new WebProcess("codemon_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )
    // new WebProcess("codemon_2", 2, 1, undefined, "http://localhost:3000/index.html", ["main.js"])];

    // Life: Realistic, works
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_LIFE,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/life/index"), 0, 0),
            new WebProcess("life_1", 1, 1, Path.join(inputProgramsFolderPath, "/life/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )
    //                                       new WebProcess("life_2", 2, 1, Path.join(__dirname, "/../../../input_programs/life/index_2.js"), "http://localhost:3000/index.html", ["main.js"])];

    addInputProgram(
      new InputProgram(
        BenchmarksEnum.HTTP_SESSION,
        [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/session/app"), 0, 0),
            new WebProcess("session_1", 1, 1, Path.join(inputProgramsFolderPath, "/session/app.js"), "http://localhost:3000/", ["index.html"])]
      )
    )

    // Insecure-chat: Realistic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_INSECURE_CHAT,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/insecure-chat/index"), 0, 0),
                new WebProcess("insecure_chat_client", 1, 1, Path.join(inputProgramsFolderPath, "/insecure-chat/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Totems: Realistic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_TOTEMS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/totems/index"), 0, 0),
                new WebProcess("totems_client", 1, 1, Path.join(inputProgramsFolderPath, "/totems/index.js"), "http://localhost:3000/index.html", ["sketch.js"])]
        )
    )

    // Tohacks: Realistic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_TOHACKS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/TOHacks/Main/server"), 0, 0),
                new WebProcess("tohacks_client", 1, 1, Path.join(inputProgramsFolderPath, "/TOHacks/Main/server.js"), "http://localhost:3000/index.html", ["game.js"])]
        )
    )

    // Savina, "Big" benchmark
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_SAVINA_BIG,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/savina/big/node_sink"), 0, 0),
            new NodeProcess("big_1", Path.join(inputProgramsFolderPath, "/savina/big/node_big_1"), 1, 1),
            new NodeProcess("big_2", Path.join(inputProgramsFolderPath, "/savina/big/node_big_2"), 2, 1),
            new NodeProcess("start", Path.join(inputProgramsFolderPath, "/savina/big/node_start"), 3, 2)]
        )
    )

    // Sebastien's bank application: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_BANK_TRANSFER,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/bank_transfer/index"), 0, 0),
            new WebProcess("bank_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )
    //                                       new WebProcess("chat_2", 2, 1, undefined, "http://localhost:3000/index.html", ["main.js"])];

    // New event-handlers (mousemove, dbclick, window events, etc.): Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_NEW_EVENTS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/bert/index"), 0, 0),
            new WebProcess("events_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // String operations: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_STRINGS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/bert_strings/index"), 0, 0),
            new WebProcess("strings_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Verify_intra_paths: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FS_VERIFY_INTRA,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/verify_intra_paths/index"), 0, 0),
            new WebProcess("verify_intra_client", 1, 1, Path.join(inputProgramsFolderPath, "/verify_intra_paths/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Thesis processes
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.THESIS_BANK_TRANSFER,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/bank_transfer/index"), 0, 0),
            new WebProcess("bank_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    /*****************************
    * Individual Process Testing *
    ******************************/

    // Calculator
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.IV_CALCULATOR,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, `/Calculator/FT/index`), 0, 0),
            new WebProcess("calc_1", 1, 1, undefined, "http://localhost:3000/index.html", [])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.OBJECTSTEST,
            [new NodeProcess("objectstest", Path.join(inputProgramsFolderPath, "/objectstest"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.RUBENTEST,
            [new NodeProcess("rubentest", Path.join(inputProgramsFolderPath, "/rubentest"), 0, 0)]
        )
    )

    // Function summaries tests
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES,
            [new NodeProcess("summaries", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_1"), 0, 0)]
        )
    )

    // Function summaries from Ward's mth
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES2,
            [new NodeProcess("summaries2", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_2"), 0, 0)]
        )
    )

    // Small variation on SUMMARIES2
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES3,
            [new NodeProcess("summaries3", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_3"), 0, 0)]
        )
    )

    // Another small variation on SUMMARIES2
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES4,
            [new NodeProcess("summaries4", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_4"), 0, 0)]
        )
    )

    // Another small variation on SUMMARIES2
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES5,
            [new NodeProcess("summaries5", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_5"), 0, 0)]
        )
    )

    // Another small variation on SUMMARIES2
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES6,
            [new NodeProcess("summaries6", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_6"), 0, 0)]
        )
    )

    // Higher-order function variation on SUMMARIES2
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.SUMMARIES7,
            [new NodeProcess("summaries7", Path.join(inputProgramsFolderPath, "/function_summaries_experiments/experiment_7"), 0, 0)]
        )
    )

    /**********************
    * Merging experiments *
    ***********************/

    /**
     * Sequential merging experiments
     */
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING1,
            [new NodeProcess("merging1", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_1"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING2,
            [new NodeProcess("merging2", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_2"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING3,
            [new NodeProcess("merging3", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_3"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING4,
            [new NodeProcess("merging4", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_4"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING5,
            [new NodeProcess("merging5", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_5"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING6,
            [new NodeProcess("merging6", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_6"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING7,
            [new NodeProcess("merging7", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_7"), 0, 0)]
        )
    )

    /**
     * Event-driven merging experiments
     */
    addInputProgram(
      new InputProgram(
        BenchmarksEnum.MERGING_EVENTS1,
        [new NodeProcess("merging_scoping1_server", Path.join(inputProgramsFolderPath, "/merging_experiments/events_experiment_1/index"), 0, 0),
            new WebProcess("merging_scoping1_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/events_experiment_1/index.js"), "http://localhost:3000/index.html", ["main.js"])]
      )
    )

    /**
     * Scoping merging experiments
     */
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING1,
            [new NodeProcess("merging_scoping1_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_1/index"), 0, 0),
                      new WebProcess("merging_scoping1_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_1/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING2,
            [new NodeProcess("merging_scoping2_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_2/index"), 0, 0),
                new WebProcess("merging_scoping2_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_2/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING3,
            [new NodeProcess("merging_scoping3_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_3/index"), 0, 0),
                new WebProcess("merging_scoping3_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_3/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING4,
            [new NodeProcess("merging_scoping4_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_4/index"), 0, 0),
                new WebProcess("merging_scoping4_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_4/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING5,
            [new NodeProcess("merging_scoping5_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_5/index"), 0, 0),
                new WebProcess("merging_scoping5_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_5/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING6,
            [new NodeProcess("merging_scoping6_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_6/index"), 0, 0),
                new WebProcess("merging_scoping6_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_6/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING7,
            [new NodeProcess("merging_scoping7_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_7/index"), 0, 0),
                new WebProcess("merging_scoping7_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_7/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING8,
            [new NodeProcess("merging_scoping8_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_8/index"), 0, 0),
                new WebProcess("merging_scoping8_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_8/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING9,
            [new NodeProcess("merging_scoping9_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_9/index"), 0, 0),
                new WebProcess("merging_scoping9_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_9/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING10,
            [new NodeProcess("merging_scoping10_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_10/index"), 0, 0),
                new WebProcess("merging_scoping10_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_10/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING11,
            [new NodeProcess("merging_scoping11_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_11/index"), 0, 0),
                new WebProcess("merging_scoping11_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_11/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.MERGING_SCOPING12,
            [new NodeProcess("merging_scoping12_server", Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_12/index"), 0, 0),
                new WebProcess("merging_scoping12_client", 1, 1, Path.join(inputProgramsFolderPath, "/merging_experiments/experiment_scoping_12/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    /****************
    * Test Fixtures *
    *****************/

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_MAIN,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/main"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_FS_WHITEBOARD,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/whiteboard/index"), 0, 0),
            new WebProcess("whiteboard_1", 1, 1, Path.join(inputProgramsFolderPath, "/fixtures/whiteboard/index_2.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    // Verify_intra_paths: Synthetic
    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_FS_VERIFY_INTRA,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/verify_intra_paths/index"), 0, 0),
            new WebProcess("verify_intra_client", 1, 1, Path.join(inputProgramsFolderPath, "/fixtures/verify_intra_paths/index.js"), "http://localhost:3000/index.html", ["main.js"])]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_NAMES_1,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/symjs/names_1"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_NAMES_2,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/symjs/names_2"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.NAMES_2,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/symjs/names_2"), 0, 0)]
        )
    )

    addInputProgram(
        new InputProgram(
            BenchmarksEnum.FIXT_2BUTTONS,
            [new NodeProcess("server", Path.join(inputProgramsFolderPath, "/fixtures/twoButtons/index"), 0, 0),
                new WebProcess("client_1", 1, 1, undefined, "http://localhost:3000/index.html", ["main.js"])]
        )
    )
}