import { Cs108Module } from './Cs108Module';

export class Cs108Manager {
  _id: string;

  constructor() {
    Cs108Module.createClient();
    this._id = 'testID';
  }

  getId() {
    return this._id;
  }
}
