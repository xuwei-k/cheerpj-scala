"use strict";

import {
  html,
  render,
  useState,
  useEffect,
  useRef,
} from "https://unpkg.com/htm@3.1.1/preact/standalone.module.js";

import { jarNames } from "./dist/jar_files.js";

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
      } else {
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
    })();
  }, [input, fileName]);

  return html`<div class="row">
      <div>
        <label for="file_name">file name: </label>
        <input
          maxlength="128"
          id="file_name"
          value=${fileName}
          oninput=${(x) => setFileName(x.target.value)}
        /><span>.scala</span>
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
