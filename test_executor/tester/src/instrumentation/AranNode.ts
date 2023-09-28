import {Node} from "estree";

interface AranInformation {
  AranParentSerial: number;
  AranSerial: number;
}

type AranNode = Node & AranInformation;

export default AranNode;