"use strict";

import {
  html,
  render,
  useState,
  useEffect,
  useRef,
} from "https://unpkg.com/htm@3.1.1/preact/standalone.module.js";

import { jarNames } from "./dist/jar_files.js";
import { Scalafmt } from "./dist/main.js";

const getFromStorageOr = (key, defaultValue, fun) => {
  const saved = localStorage.getItem(key);
  if (saved === null) {
    return defaultValue;
  } else {
    if (fun == null) {
      return saved;
    } else {
      return fun(saved);
    }
  }
};

const initialInput = getFromStorageOr(
  "input",
  `package example

object Main {
  def main(args: Array[String]): Unit = {
    println("hello")
  }
}
`,
);
const initialFileName = getFromStorageOr("file_name", "Main");

const App = () => {
  const [autoRun, setAutoRun] = useState(true);
  const [fileName, setFileName] = useState(initialFileName);
  const [output, setOutput] = useState("");
  const [input, setInput] = useState(initialInput);
  const mainRef = useRef(null);
  const running = useRef(false);
  const cm = useRef(null);

  useEffect(() => {
    if (cm.current === null) {
      cm.current = CodeMirror(document.getElementById("input"), {
        lineNumbers: true,
        matchBrackets: true,
        value: input,
        mode: "text/x-scala",
      });
      cm.current.setSize("100%", "100%");
    }
    return () => {};
  }, [input]);

  async function formatInput() {
    const code = Scalafmt.format(input);
    setInput(code);
    cm.current.setValue(code);
  }

  async function compileAndRun() {
    running.current = true;

    try {
      const encoder = new TextEncoder();
      const baseDir = (Math.random() + 1).toString(36).substring(2);
      const scalaFile = `/str/${baseDir}/${fileName}.scala`;
      cheerpOSAddStringFile(scalaFile, encoder.encode(input));
      const main = mainRef.current;
      const classOutput = `/files/${baseDir}/`;
      const classpath = jarNames.map((x) => "/app/cheerpj-scala/dist/" + x);
      const result = await main.runMain(scalaFile, classOutput, classpath);
      setOutput(result);

      [
        ["input", input],
        ["file_name", fileName],
      ].forEach(([key, val]) => {
        if (val.toString().length <= 1024 * 8) {
          localStorage.setItem(key, val);
        }
      });
    } finally {
      running.current = false;
    }
  }

  useEffect(() => {
    (async () => {
      if (mainRef.current === null) {
        setOutput("loading ...");
        await cheerpjInit();
        const lib = await cheerpjRunLibrary(
          jarNames.map((x) => "/app/cheerpj-scala/dist/" + x).join(":"),
        );
        mainRef.current = await lib.cheerpj_scala.Main;
      }
      if (running.current === true) {
        console.log("skip");
      } else if (autoRun) {
        await compileAndRun();
      }
    })();
  }, [input, fileName]);

  return html`<div class="row">
      <div class="col-3">
        <label for="file_name">file name: </label>
        <input
          maxlength="128"
          id="file_name"
          value=${fileName}
          oninput=${(x) => setFileName(x.target.value)}
        /><span>.scala</span>
      </div>
      <div class="col-1">
        <div>
          <input
            type="checkbox"
            name="auto_run"
            id="auto_run"
            checked=${autoRun}
            onChange=${(e) => setAutoRun(e.target.checked)}
          />
          <label for="auto_run">auto run</label>
        </div>
      </div>
      <div class="col-1">
        <button
          class="btn btn-primary"
          id="run"
          onclick=${() => compileAndRun()}
        >
          run
        </button>
      </div>
      <div class="col">
        <button class="btn btn-primary" onclick=${() => formatInput()}>
          format input scala code
        </button>
      </div>
    </div>
    <div class="row" style="height: 800px;">
      <div class="col">
        <div
          id="input"
          style="width: 100%; height: 100%;"
          onkeyup=${(e) => setInput(cm.current.getValue())}
          onChange=${(e) => setInput(cm.current.getValue())}
        ></div>
      </div>
      <div class="col">
        <pre
          style="width: 100%; height: 100%; background-color:rgb(66, 66, 66);"
        >
${output}</pre
        >
      </div>
    </div>`;
};

render(html`<${App} />`, document.getElementById("root"));
